/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.privacy;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Reconciles server-side unread IDs with device-local ghost state.
 * Only getUnreadMentions/getUnreadReactions are sent: never read receipts.
 * An incomplete result is null, NOT an empty set, so callers keep local state.
 */
public final class FinegramGhostAttentionSync {
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 100;
    private static final long TIMEOUT_MS = 30000;
    private FinegramGhostAttentionSync() {}

    public static void load(int account, long dialogId, long topicId, boolean reactions,
            BooleanSupplier permitted, Consumer<TLRPC.Message> message,
            Consumer<Set<Integer>> completion) {
        AndroidUtilities.runOnUIThread(() -> new Load(account, dialogId, topicId, reactions,
                permitted, message, completion).start());
    }

    static boolean validCursor(int previous, int next) {
        return previous >= 0 && next > 0 && (previous == 0 || next < previous);
    }

    private static final class Load {
        final int account;
        final long dialogId, topicId;
        final boolean reactions;
        final BooleanSupplier permitted;
        final Consumer<TLRPC.Message> message;
        final Consumer<Set<Integer>> completion;
        final Set<Integer> ids = new HashSet<>();
        final Runnable timeout = () -> finish(null);
        int requestId, pages;
        boolean finished;

        Load(int account, long dialogId, long topicId, boolean reactions,
                BooleanSupplier permitted, Consumer<TLRPC.Message> message,
                Consumer<Set<Integer>> completion) {
            this.account = account; this.dialogId = dialogId; this.topicId = topicId;
            this.reactions = reactions; this.permitted = permitted;
            this.message = message; this.completion = completion;
        }
        void start() {
            if (!permitted.getAsBoolean()) { finish(null); return; }
            AndroidUtilities.runOnUIThread(timeout, TIMEOUT_MS);
            next(0);
        }
        void next(int offset) {
            if (finished) return;
            if (!permitted.getAsBoolean() || ++pages > MAX_PAGES || topicId < 0) { finish(null); return; }
            MessagesController controller = MessagesController.getInstance(account);
            TLRPC.InputPeer peer = controller.getInputPeer(dialogId);
            if (peer == null || peer instanceof TLRPC.TL_inputPeerEmpty) { finish(null); return; }
            TLObject request;
            if (reactions) {
                TLRPC.TL_messages_getUnreadReactions r = new TLRPC.TL_messages_getUnreadReactions();
                r.peer = peer; r.limit = PAGE_SIZE; r.offset_id = offset;
                if (topicId != 0 && controller.isMonoForum(dialogId)) {
                    r.saved_peer_id = controller.getInputPeer(topicId);
                    if (r.saved_peer_id == null || r.saved_peer_id instanceof TLRPC.TL_inputPeerEmpty) { finish(null); return; }
                    r.flags |= 2;
                } else if (topicId != 0) {
                    if (topicId > Integer.MAX_VALUE) { finish(null); return; }
                    r.top_msg_id = (int) topicId; r.flags |= 1;
                }
                request = r;
            } else {
                // This endpoint has no saved_peer_id filter; do not query the wrong topic.
                if (topicId > Integer.MAX_VALUE || topicId != 0 && controller.isMonoForum(dialogId)) { finish(null); return; }
                TLRPC.TL_messages_getUnreadMentions r = new TLRPC.TL_messages_getUnreadMentions();
                r.peer = peer; r.limit = PAGE_SIZE; r.offset_id = offset;
                if (topicId != 0) { r.top_msg_id = (int) topicId; r.flags |= 1; }
                request = r;
            }
            requestId = ConnectionsManager.getInstance(account).sendRequest(request, (response, error) ->
                AndroidUtilities.runOnUIThread(() -> {
                    requestId = 0;
                    if (finished) return;
                    if (!permitted.getAsBoolean() || error != null || !(response instanceof TLRPC.messages_Messages)) {
                        finish(null); return;
                    }
                    TLRPC.messages_Messages result = (TLRPC.messages_Messages) response;
                    if (result.messages == null) { finish(null); return; }
                    if (result.messages.isEmpty()) {
                        finish(result.count > ids.size() ? null : ids); return;
                    }
                    int nextOffset = 0;
                    for (TLRPC.Message item : result.messages) {
                        if (!permitted.getAsBoolean()) { finish(null); return; }
                        if (item.id <= 0) continue;
                        if (nextOffset == 0 || item.id < nextOffset) nextOffset = item.id;
                        if (item instanceof TLRPC.TL_messageEmpty) continue;
                        // Capture the server ID before the caller changes unread flags locally.
                        ids.add(item.id); item.dialog_id = dialogId; message.accept(item);
                    }
                    if (!validCursor(offset, nextOffset)) { finish(null); return; }
                    if (result.messages.size() < PAGE_SIZE && result.count <= ids.size()) {
                        finish(ids); return;
                    }
                    next(nextOffset);
                }));
        }
        void finish(Set<Integer> result) {
            if (finished) return;
            finished = true;
            AndroidUtilities.cancelRunOnUIThread(timeout);
            if (requestId != 0) {
                ConnectionsManager.getInstance(account).cancelRequest(requestId, true); requestId = 0;
            }
            completion.accept(result == null ? null : Collections.unmodifiableSet(new HashSet<>(result)));
        }
    }
}
