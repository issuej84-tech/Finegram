/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.materialregression;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/** Run only against the disposable full debug client in an emulator, never an account device. */
public final class FullWallpaperRegression extends Instrumentation {
    int checks; boolean liveNetwork;
    void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    Class<?> type(String name)throws Exception{return getTargetContext().getClassLoader().loadClass(name);}
    void preference(Object settings,String name,Class<?> arg,Object value)throws Exception{settings.getClass().getMethod(name,arg).invoke(settings,value);}
    int difference(int a,int b){return Math.max(Math.abs(Color.red(a)-Color.red(b)),Math.max(Math.abs(Color.green(a)-Color.green(b)),Math.abs(Color.blue(a)-Color.blue(b))));}
    @Override public void onCreate(Bundle args){super.onCreate(args);liveNetwork=args!=null&&"true".equalsIgnoreCase(args.getString("live_network"));start();}
    @Override public void onStart(){Bundle result=new Bundle();Activity activity=null;try{
        Context context=getTargetContext();
        Class<?> settingsClass=type("com.th3nekit.finegram.appearance.material.MaterialSettings");
        Object settings=settingsClass.getConstructor(Context.class).newInstance(context);
        Class<?> appearance=type("com.th3nekit.finegram.appearance.material.MaterialAppearance");
        Method hidden=appearance.getMethod("hideStockHeaderCovers");
        preference(settings,"enabled",boolean.class,true);
        check((Boolean)hidden.invoke(null),"Stock covers visible in Material mode");
        preference(settings,"enabled",boolean.class,false);preference(settings,"wallpaper",boolean.class,false);
        check(!(Boolean)hidden.invoke(null),"Original cover preference was destroyed");
        preference(settings,"wallpaper",boolean.class,true);
        check((Boolean)hidden.invoke(null),"Stock covers visible over a user wallpaper");
        preference(settings,"enabled",boolean.class,true);
        File source=new File(context.getCacheDir(),"material-window-regression.png");
        Bitmap pattern=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888);
        for(int y=0;y<600;y++)for(int x=0;x<400;x++)pattern.setPixel(x,y,Color.rgb(x*255/400,y*255/600,(x+y)*255/1000));
        try(FileOutputStream out=new FileOutputStream(source)){pattern.compress(Bitmap.CompressFormat.PNG,100,out);}pattern.recycle();
        Class<?> wallpaper=type("com.th3nekit.finegram.appearance.material.MaterialWallpaper");
        CountDownLatch done=new CountDownLatch(1);boolean[] imported={false,false};
        Class<?> callbackType=type("com.th3nekit.finegram.appearance.material.MaterialWallpaper$Result");
        Object callback=java.lang.reflect.Proxy.newProxyInstance(callbackType.getClassLoader(),new Class<?>[]{callbackType},(proxy,method,args)->{if(method.getName().equals("done")){imported[0]=(Boolean)args[0];imported[1]=Looper.myLooper()==Looper.getMainLooper();done.countDown();}return null;});
        wallpaper.getMethod("importImage",Context.class,Uri.class,callbackType).invoke(null,context,Uri.fromFile(source),callback);
        check(done.await(15,TimeUnit.SECONDS),"Import timed out");check(imported[0]&&imported[1],"Import/main-thread callback failed");
        Drawable drawable=(Drawable)appearance.getMethod("chatWallpaper").invoke(null);
        check(drawable!=null,"Global chat wallpaper is missing after import");
        Method draw=wallpaper.getMethod("drawInView",Drawable.class,Canvas.class,View.class);
        activity=startActivitySync(new Intent().setClassName(context.getPackageName(),"com.th3nekit.finegram.appearance.material.MaterialStudioActivity").putExtra("guide",false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();Thread.sleep(300);
        final Activity current=activity;FrameLayout[] views=new FrameLayout[2];Throwable[] uiError=new Throwable[1];
        Class<?> sizeNotifier=type("org.telegram.ui.Components.SizeNotifierFrameLayout");
        runOnMainSync(()->{try{FrameLayout root=new FrameLayout(current),child=(FrameLayout)sizeNotifier.getConstructor(Context.class).newInstance(current);sizeNotifier.getMethod("setBackgroundImage",Drawable.class,boolean.class).invoke(child,drawable,true);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(350,700);lp.leftMargin=80;lp.topMargin=140;root.addView(child,lp);current.setContentView(root);views[0]=root;views[1]=child;}catch(Throwable e){uiError[0]=e;}});
        if(uiError[0]!=null)throw new AssertionError("UI regression failed",uiError[0]);
        waitForIdleSync();Thread.sleep(150);
        runOnMainSync(()->{try{
            FrameLayout root=views[0],child=views[1];check(root.getWidth()>700&&root.getHeight()>1000,"Unexpected emulator viewport");
            Bitmap whole=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
            check((Boolean)draw.invoke(null,drawable,new Canvas(whole),root),"Full window drawing not handled");
            for(int frame=0;frame<24;frame++){
                child.setTranslationX(frame*7);child.setTranslationY(frame*3);
                drawable.setBounds(-999,-333,23,44); // Legacy parallax/bounds must have no effect.
                Bitmap part=Bitmap.createBitmap(child.getWidth(),child.getHeight(),Bitmap.Config.ARGB_8888);
                check((Boolean)draw.invoke(null,drawable,new Canvas(part),child),"Chat drawing not handled");
                int[] a=new int[2],b=new int[2];root.getLocationInWindow(a);child.getLocationInWindow(b);
                for(int y:new int[]{30,180,400,650})for(int x:new int[]{25,100,200,300})
                    check(difference(whole.getPixel(x+b[0]-a[0],y+b[1]-a[1]),part.getPixel(x,y))<=3,"Wallpaper shifted at transition frame "+frame);
                Bitmap integrated=Bitmap.createBitmap(child.getWidth(),child.getHeight(),Bitmap.Config.ARGB_8888);
                child.draw(new Canvas(integrated));
                for(int y:new int[]{30,180,400,650})for(int x:new int[]{25,100,200,300})
                    check(difference(part.getPixel(x,y),integrated.getPixel(x,y))<=3,"Chat BackgroundView/parallax path shifted at frame "+frame);
                integrated.recycle();part.recycle();
            }
            check(!(Boolean)draw.invoke(null,new android.graphics.drawable.ColorDrawable(Color.RED),new Canvas(whole),root),"Per-chat/stock wallpaper was intercepted");
            whole.recycle();
        }catch(Throwable e){uiError[0]=e;}});
        if(uiError[0]!=null)throw new AssertionError("UI regression failed",uiError[0]);
        Class<?> config=type("com.th3nekit.finegram.net.bypass.FGBypassConfig");
        int previous=config.getField("source").getInt(null);config.getMethod("setSource",int.class).invoke(null,0);
        check((Boolean)config.getMethod("tgwsFirst").invoke(null),"Flowseal route not selected");
        check(!(Boolean)type("com.th3nekit.finegram.net.bypass.WsRelayAuth").getMethod("isAuthAllowed").invoke(null),"Finegram relay auth allowed in Flowseal mode");
        Class<?> coreClass=type("com.th3nekit.finegram.net.bypass.WsBypassCore");
        Object core=coreClass.getMethod("getInstance").invoke(null);
        Method relay=coreClass.getDeclaredMethod("tryRelay",int.class,boolean.class,long.class,long.class,boolean.class);relay.setAccessible(true);
        check(relay.invoke(core,2,false,System.nanoTime()+TimeUnit.SECONDS.toNanos(2),0L,true)==null,"Flowseal silently entered Finegram relay");
        if(liveNetwork){
            String status;
            try{
                Class<?> permitClass=type("com.th3nekit.finegram.net.bypass.RawWebSocket$ConnectPermit");
                Object permit=java.lang.reflect.Proxy.newProxyInstance(permitClass.getClassLoader(),new Class<?>[]{permitClass},(proxy,method,args)->Boolean.TRUE);
                Method connect=type("com.th3nekit.finegram.net.bypass.TgwsRoute").getDeclaredMethod("connect",int.class,long.class,permitClass);connect.setAccessible(true);
                long began=android.os.SystemClock.elapsedRealtime();
                Object socket=connect.invoke(null,2,System.nanoTime()+TimeUnit.SECONDS.toNanos(8),permit);
                socket.getClass().getMethod("close").invoke(socket);
                status="PASS TLS + HTTP 101 in "+(android.os.SystemClock.elapsedRealtime()-began)+" ms";
            }catch(Throwable network){Throwable reason=network.getCause()==null?network:network.getCause();status="UNAVAILABLE: "+reason.getClass().getSimpleName()+": "+reason.getMessage();}
            result.putString("live_flowseal",status);
            android.util.Log.i("FinegramRegression","LIVE_FLOWSEAL_WS="+status);
        }
        config.getMethod("setSource",int.class).invoke(null,previous);
        source.delete();
        result.putString("stream","PASS: "+checks+" full-client regressions: stock-cover policy, shared window crop through 24 transition frames, no legacy parallax, per-chat wallpaper isolation and Flowseal auth isolation.\n");
        finish(Activity.RESULT_OK,result);
    }catch(Throwable e){java.io.StringWriter trace=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(trace));result.putString("stream","FAIL: "+trace+"\n");finish(Activity.RESULT_CANCELED,result);}}
}
