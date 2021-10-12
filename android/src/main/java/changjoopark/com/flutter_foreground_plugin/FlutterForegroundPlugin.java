package changjoopark.com.flutter_foreground_plugin;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterForegroundPlugin
 */
public class FlutterForegroundPlugin implements FlutterPlugin, MethodCallHandler {
    public final static String START_FOREGROUND_ACTION = "com.changjoopark.flutter_foreground_plugin.action.startforeground";
    public final static String STOP_FOREGROUND_ACTION = "com.changjoopark.flutter_foreground_plugin.action.stopforeground";

    private static FlutterForegroundPlugin instance;

    private Context context;
    private MethodChannel callbackChannel;
    private long methodInterval = -1;
    private long dartServiceMethodHandle = -1;
    private boolean serviceStarted = false;
    private Runnable runnable;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        System.out.println("onAttachedToEnginem (flutter binding) called!!");
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    public void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        System.out.println("onAttachedToEngine called!!");
        this.context = applicationContext;
        final MethodChannel channel = new MethodChannel(messenger, "com.changjoopark.flutter_foreground_plugin/main");
        channel.setMethodCallHandler(this);
        callbackChannel = new MethodChannel(messenger, "com.changjoopark.flutter_foreground_plugin/callback");
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        System.out.println("onDetachedFromEngine called!!");
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        if (instance == null) {
            instance = new FlutterForegroundPlugin();
        }
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "startForegroundService":
                final String icon = call.argument("icon");

                Number colorBox = call.argument("color");
                final int color = colorBox == null ? NotificationCompat.COLOR_DEFAULT : ((Number) colorBox).intValue();

                final String title = call.argument("title");
                final String content = call.argument("content");
                final String subtext = call.argument("subtext");

                Boolean chronometerBox = call.argument("chronometer");
                final boolean chronometer = chronometerBox != null && chronometerBox;

                Boolean stopActionBox = call.argument("stop_action");
                final boolean stopAction = stopActionBox != null && stopActionBox;

                final String stopIcon = call.argument("stop_icon");
                final String stopText = call.argument("stop_text");

                launchForegroundService(icon, color, title, content, subtext, chronometer, stopAction, stopIcon, stopText);
                result.success("startForegroundService");
                break;
            case "stopForegroundService":
                stopForegroundService();
                result.success("stopForegroundService");
                break;
            case "setServiceMethodInterval":
                Number secondsBox = call.argument("seconds");
                if (secondsBox == null) {
                    result.notImplemented();
                    break;
                }
                methodInterval = secondsBox.longValue();
                result.success("setServiceMethodInterval");
                break;
            case "setServiceMethodHandle":
                Number methodHandleBox = call.argument("serviceMethodHandle");
                if (methodHandleBox == null) {
                    result.notImplemented();
                    break;
                }
                dartServiceMethodHandle = methodHandleBox.longValue();

                result.success("setServiceMethodHandle");
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void launchForegroundService(String icon, int color, String title, String content, String subtext,
                                         boolean chronometer, boolean stopAction, String stopIcon,
                                         String stopText) {
        Intent intent = new Intent(context, FlutterForegroundService.class);
        intent.setAction(START_FOREGROUND_ACTION);
        intent.putExtra("icon", icon);
        intent.putExtra("color", color);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("subtext", subtext);
        intent.putExtra("chronometer", chronometer);
        intent.putExtra("stop_action", stopAction);
        intent.putExtra("stop_icon", stopIcon);
        intent.putExtra("stop_text", stopText);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        serviceStarted = true;
        startServiceLoop();

        callbackChannel.invokeMethod("onStarted", null);
    }

    /**
     *
     */
    private void stopForegroundService() {
        serviceStarted = false;

        if (handler != null) {
            handler.removeCallbacks(runnable);
        }

        dartServiceMethodHandle = -1;
        methodInterval = -1;

        Intent intent = new Intent(context, FlutterForegroundService.class);
        intent.setAction(STOP_FOREGROUND_ACTION);
        context.startService(intent);
        callbackChannel.invokeMethod("onStopped", null);
    }

    /**
     *
     */
    private void startServiceLoop() {
        if (dartServiceMethodHandle == -1 || methodInterval == -1) {
            return;
        }

        final long interval = methodInterval * 1000;

        if (runnable == null) {
            runnable = new Runnable() {
                public void run() {
                    if (!serviceStarted) {
                        return;
                    }
                    try {
                        callbackChannel.invokeMethod("onServiceMethodCallback", dartServiceMethodHandle);
                    } catch (Error e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, interval);
                }
            };
        }


        if (handler != null) {
            handler.removeCallbacks(runnable);
        }

        handler = new Handler();
        handler.postDelayed(runnable, interval);
    }
}
