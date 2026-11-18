package dev.cartdeck.pc.lab;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import com.swordfish.libretrodroid.GLRetroView;
import com.swordfish.libretrodroid.GLRetroViewData;
import java.io.File;

public final class LabSessionActivity extends ComponentActivity {
    private GLRetroView view;
    private TextView status;
    private boolean active, mouseTracking;
    private float lastX, lastY;
    private int mouseButtons;
    private long inputGeneration;
    private final android.hardware.input.InputManager.InputDeviceListener deviceListener = new android.hardware.input.InputManager.InputDeviceListener() {
        public void onInputDeviceAdded(int id) { release(); }
        public void onInputDeviceChanged(int id) { release(); }
        public void onInputDeviceRemoved(int id) { release(); }
    };
    private final int KEY = 10000, BUTTON = 10600, CLEAR = 10610;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            LabWorkspace workspace = new LabWorkspace(getFilesDir(), getIntent().getStringExtra("workspace"));
            File content = new File(workspace.media, getIntent().getBooleanExtra("synthetic", false) ? "probe.dosz" : "guest.dosz");
            if (!content.isFile()) throw new IllegalStateException("Media missing");
            GLRetroViewData data = new GLRetroViewData(this);
            data.setCoreFilePath(new File(getApplicationInfo().nativeLibraryDir, "libdosbox_pure_libretro_android.so").getAbsolutePath());
            data.setGameFilePath(content.getAbsolutePath());
            data.setSystemDirectory(workspace.system.getAbsolutePath());
            data.setSavesDirectory(workspace.saves.getAbsolutePath());
            if (getIntent().getBooleanExtra("windows",false)) {
                data.setVariables(new com.swordfish.libretrodroid.Variable[]{
                    new com.swordfish.libretrodroid.Variable("dosbox_pure_memory_size","64",null),
                    new com.swordfish.libretrodroid.Variable("dosbox_pure_bootos_ramdisk","false",null),
                    new com.swordfish.libretrodroid.Variable("dosbox_pure_bootos_dfreespace","1024",null)
                });
            }
            view = new GLRetroView(this, data);
            getLifecycle().addObserver(view);

            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            status = new TextView(this);
            status.setText("PC Lab only — wait for guest display; shut down within guest before exit");
            page.addView(status);
            page.addView(view, new LinearLayout.LayoutParams(-1, 0, 1));
            LinearLayout controls = new LinearLayout(this);
            button(controls, "Discs", () -> disk(false));
            button(controls, "Next disc", () -> disk(true));
            button(controls, "Keys", this::keyboard);
            button(controls, "Exit", this::confirmExit);
            page.addView(controls);
            LinearLayout mouse = new LinearLayout(this);
            button(mouse, "Left click", () -> click(0));
            button(mouse, "Right click", () -> click(1));
            button(mouse, "Special keys", this::specialKeys);
            page.addView(mouse);
            view.setOnTouchListener((v,e) -> {
                if (!active) return true;
                if (e.isFromSource(InputDevice.SOURCE_MOUSE)) return handleMouse(e);
                int a=e.getActionMasked();
                if (a==MotionEvent.ACTION_DOWN) { lastX=e.getX(); lastY=e.getY(); mouseTracking=true; }
                else if (a==MotionEvent.ACTION_MOVE && mouseTracking) {
                    relative(e.getX()-lastX,e.getY()-lastY); lastX=e.getX(); lastY=e.getY();
                } else if(a==MotionEvent.ACTION_UP || a==MotionEvent.ACTION_CANCEL) mouseTracking=false;
                return true;
            });
            setContentView(page);
        } catch (Exception e) {
            status = new TextView(this); status.setText("PC lab initialization error: "+e.getMessage()); setContentView(status);
        }
    }
    private void button(LinearLayout row, String text, Runnable task) {
        Button b=new Button(this); b.setText(text); b.setOnClickListener(v->task.run());
        row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void disk(boolean next) {
        if (!active || view==null) return;
        // All native disk callbacks run on the GL thread; never block the UI on a latch.
        view.queueEvent(() -> {
            try {
                int count=view.getAvailableDisks(false);
                int current=count>0?view.getCurrentDisk(false):-1;
                if(next && count>1) { view.changeDisk((current+1)%count,false); current=view.getCurrentDisk(false); }
                final String message=count>0?"Core reports disc "+(current+1)+" of "+count:"No core optical control reported";
                runOnUiThread(()->{if(!isFinishing())status.setText(message);});
            } catch (RuntimeException e) {runOnUiThread(()->status.setText("Disc operation failed"));}
        });
    }
    private void relative(float x,float y) {
        if(active && view!=null && Float.isFinite(x) && Float.isFinite(y))view.sendMotionEvent(5,x,y,0);
    }
    private void click(int index) {
        if(!active||view==null)return;
        if (!hasWindowFocus()) return;
        final long generation=inputGeneration;
        view.sendKeyEvent(KeyEvent.ACTION_DOWN,BUTTON+index,0);
        view.postDelayed(()->{if(active && generation==inputGeneration)view.sendKeyEvent(KeyEvent.ACTION_UP,BUTTON+index,0);},100);
    }
    private void confirmExit(){
        new android.app.AlertDialog.Builder(this)
            .setMessage("Exit only after guest shutdown or writes are complete. Forced exit is not a verified clean shutdown.")
            .setNegativeButton("Cancel",null).setPositiveButton("Exit lab",(d,w)->{release();finish();}).show();
    }
    @Override public void onBackPressed(){confirmExit();}
    private void keyboard(){
        if(!active)return;
        EditText field=new EditText(this); field.setSingleLine(true);
        new android.app.AlertDialog.Builder(this).setTitle("Send basic ASCII to guest").setView(field)
            .setNegativeButton("Cancel",null).setPositiveButton("Send",(d,w)->{
                String text=field.getText().toString().toLowerCase(java.util.Locale.ROOT);
                scheduleKeys(text);
            }).setNeutralButton("Enter",(d,w)->scheduleKeys("\n")).show();
    }
    private void specialKeys(){
        if(!active)return;
        String[] names={"Enter","Escape","Tab","Backspace","Up","Down","Left","Right","Space","F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12"};
        int[] keys={13,27,9,8,273,274,276,275,32,282,283,284,285,286,287,288,289,290,291,292,293};
        new android.app.AlertDialog.Builder(this).setTitle("Guest key").setItems(names,(d,index)->{
            final long generation=inputGeneration;
            view.postDelayed(()->{if(active&&hasWindowFocus()&&generation==inputGeneration){
                view.sendKeyEvent(0,KEY+keys[index],0);
                view.postDelayed(()->{if(active&&generation==inputGeneration)view.sendKeyEvent(1,KEY+keys[index],0);},100);
            }},300);
        }).show();
    }
    private void scheduleKeys(String text){
        final long generation=inputGeneration;
        for(int i=0;i<Math.min(text.length(),128);i++){
            char c=text.charAt(i);
            if(!(c>='a'&&c<='z')&&!(c>='0'&&c<='9')&&" .\\/-_:\n".indexOf(c)<0)continue;
            final int code=c=='\n'?13:c==':'?';':c=='_'?'-':c;
            final boolean shift=c==':'||c=='_';
            view.postDelayed(()->{
                if(active&&hasWindowFocus()&&generation==inputGeneration){
                    if(shift)view.sendKeyEvent(0,KEY+304,0);
                    view.sendKeyEvent(0,KEY+code,0);
                    view.postDelayed(()->{if(active&&generation==inputGeneration){view.sendKeyEvent(1,KEY+code,0);if(shift)view.sendKeyEvent(1,KEY+304,0);}},70);
                }
            },250+i*140L);
        }
    }
    private static int retroKey(int code){
        if(code>=KeyEvent.KEYCODE_A&&code<=KeyEvent.KEYCODE_Z)return 'a'+code-KeyEvent.KEYCODE_A;
        if(code>=KeyEvent.KEYCODE_0&&code<=KeyEvent.KEYCODE_9)return '0'+code-KeyEvent.KEYCODE_0;
        if(code>=KeyEvent.KEYCODE_F1&&code<=KeyEvent.KEYCODE_F12)return 282+code-KeyEvent.KEYCODE_F1;
        switch(code){case KeyEvent.KEYCODE_ENTER:return 13;case KeyEvent.KEYCODE_SPACE:return 32;case KeyEvent.KEYCODE_DEL:return 8;
        case KeyEvent.KEYCODE_PERIOD:return '.';case KeyEvent.KEYCODE_SLASH:return '/';case KeyEvent.KEYCODE_BACKSLASH:return 92;case KeyEvent.KEYCODE_SEMICOLON:return ';';case KeyEvent.KEYCODE_MINUS:return '-';
        case KeyEvent.KEYCODE_ALT_LEFT:return 308;case KeyEvent.KEYCODE_ALT_RIGHT:return 307;
        case KeyEvent.KEYCODE_ESCAPE:return 27;case KeyEvent.KEYCODE_TAB:return 9;case KeyEvent.KEYCODE_DPAD_UP:return 273;
        case KeyEvent.KEYCODE_DPAD_DOWN:return 274;case KeyEvent.KEYCODE_DPAD_RIGHT:return 275;case KeyEvent.KEYCODE_DPAD_LEFT:return 276;
        case KeyEvent.KEYCODE_SHIFT_LEFT:return 304;case KeyEvent.KEYCODE_SHIFT_RIGHT:return 303;
        case KeyEvent.KEYCODE_CTRL_LEFT:return 306;case KeyEvent.KEYCODE_CTRL_RIGHT:return 305;
        default:return -1;}
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(active&&view!=null&&e.isFromSource(InputDevice.SOURCE_KEYBOARD)&&!e.isFromSource(InputDevice.SOURCE_GAMEPAD)){
            int k=retroKey(e.getKeyCode());
            if(k>=0&&(e.getAction()==0||e.getAction()==1)){
                if(e.getRepeatCount()==0||e.getAction()==1)view.sendKeyEvent(e.getAction(),KEY+k,0);return true;
            }
        }
        if(active&&e.isFromSource(InputDevice.SOURCE_GAMEPAD)&&e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_A){
            view.sendKeyEvent(e.getAction(),BUTTON,0);return true;
        }
        return super.dispatchKeyEvent(e);
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){
        if(active&&view!=null&&e.isFromSource(InputDevice.SOURCE_MOUSE))return handleMouse(e);
        if(active&&view!=null&&e.isFromSource(InputDevice.SOURCE_JOYSTICK)&&e.getActionMasked()==MotionEvent.ACTION_MOVE){
            float x=e.getAxisValue(MotionEvent.AXIS_X),y=e.getAxisValue(MotionEvent.AXIS_Y);
            relative(Math.abs(x)>.15?x*10:0,Math.abs(y)>.15?y*10:0);return true;
        }
        return super.dispatchGenericMotionEvent(e);
    }
    private boolean handleMouse(MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){release();return true;}
            if(mouseTracking && e.getActionMasked()!=MotionEvent.ACTION_HOVER_ENTER)relative(e.getX()-lastX,e.getY()-lastY);
            lastX=e.getX();lastY=e.getY();mouseTracking=e.getActionMasked()!=MotionEvent.ACTION_HOVER_EXIT;
            int mask=e.getButtonState(), delta=mask^mouseButtons;
            int[] buttons={MotionEvent.BUTTON_PRIMARY,MotionEvent.BUTTON_SECONDARY,MotionEvent.BUTTON_TERTIARY};
            for(int i=0;i<buttons.length;i++)if((delta&buttons[i])!=0)view.sendKeyEvent((mask&buttons[i])!=0?0:1,BUTTON+i,0);
            mouseButtons=mask;return true;
    }
    private void release(){inputGeneration++;if(view!=null&&active)view.sendKeyEvent(0,CLEAR,0);mouseButtons=0;mouseTracking=false;}
    @Override protected void onResume(){
        super.onResume();active=view!=null;
        ((android.hardware.input.InputManager)getSystemService(INPUT_SERVICE)).registerInputDeviceListener(deviceListener,new android.os.Handler(android.os.Looper.getMainLooper()));
    }
    @Override protected void onPause(){
        ((android.hardware.input.InputManager)getSystemService(INPUT_SERVICE)).unregisterInputDeviceListener(deviceListener);
        release();active=false;super.onPause();
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused)release();}
}
