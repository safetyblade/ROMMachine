package com.swordfish.lemuroid.app.pc;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import dev.cartdeck.pc.runtime.GLRetroView;
import dev.cartdeck.pc.runtime.GLRetroViewData;
import java.io.File;
import android.content.Intent;
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity;
import com.swordfish.lemuroid.app.shared.game.GameProcessLock;
import com.swordfish.lemuroid.app.shared.game.pc.PcManagedSession;
import com.swordfish.lemuroid.lib.library.db.entity.Game;
import com.swordfish.lemuroid.lib.library.pc.PcCartridgeStore;
import com.swordfish.lemuroid.lib.library.pc.PcWindowsLaunchRecipe;
import com.swordfish.lemuroid.lib.library.pc.PcWorkspace;

public final class PcIntegratedActivity extends ComponentActivity {
    private Game game;
    private PcCartridgeStore.Entry entry;
    private PcCartridgeStore store;
    private PcWorkspace workspace;
    private FrameLayout frame;
    private View menu;
    private long startTime;
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
            startTime=android.os.SystemClock.elapsedRealtime();
            game=(Game)getIntent().getSerializableExtra("cartdeck.game");
            if(game==null)throw new IllegalStateException("Missing cartridge");
            if(!PcManagedSession.INSTANCE.permits(PcManagedSession.INSTANCE.getLocalOwner(),false))throw new IllegalStateException("Profile denied");
            entry=PcManagedSession.INSTANCE.resolve(this,game);
            if(entry==null)throw new IllegalStateException("Not managed PC media");
            GameProcessLock.INSTANCE.acquireExclusive(getApplicationContext());
            store=new PcCartridgeStore(getFilesDir());
            workspace=store.workspace(entry);
            workspace.prepareWritableDirectories();
            File content=entry.getContent();
            if (!content.isFile()) throw new IllegalStateException("Media missing");
            GLRetroViewData data = new GLRetroViewData(this);
            data.setCoreFilePath(new File(getApplicationInfo().nativeLibraryDir, "libcartdeckpc_dosbox.so").getAbsolutePath());
            data.setGameFilePath(content.getAbsolutePath());
            data.setSystemDirectory(workspace.getGuest().getAbsolutePath());
            data.setSavesDirectory(workspace.getProgress().getAbsolutePath());
            {
                data.setVariables(new dev.cartdeck.pc.runtime.Variable[]{
                    new dev.cartdeck.pc.runtime.Variable("dosbox_pure_memory_size","64",null),
                    new dev.cartdeck.pc.runtime.Variable("dosbox_pure_bootos_ramdisk","false",null),
                    new dev.cartdeck.pc.runtime.Variable("dosbox_pure_bootos_dfreespace","1024",null)
                });
            }
            view = new GLRetroView(this, data);
            getLifecycle().addObserver(view);

            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            status = new TextView(this);
            status.setText("CARTDECK PC • "+game.displayName()+" — shut down in the guest before exit");
            page.addView(status);
            page.addView(view, new LinearLayout.LayoutParams(-1, 0, 1));
            LinearLayout controls = new LinearLayout(this);
            button(controls, "Discs", () -> disk(false));
            button(controls, "Next disc", () -> disk(true));
            button(controls, "Keys", this::keyboard);
            button(controls, "CARTDECK", this::openMenu);
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
            frame=new FrameLayout(this);frame.addView(page);setContentView(frame);
        } catch (Exception e) {
            setResult(BaseGameActivity.RESULT_ERROR,new Intent().putExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR,"PC runtime could not start. Existing media and saves retained."));finish();
        }
    }
    private void button(LinearLayout row, String text, Runnable task) {
        Button b=new Button(this); b.setText(text); b.setOnClickListener(v->task.run());
        row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void disk(boolean next) {
        if (!active || view==null || menu!=null) return;
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
    private void openMenu(){
        if(view==null||menu!=null||frame==null)return;
        release();
        menu=PcMenuOverlay.create(this,game.displayName(),this::closeMenu,
            ()->{closeMenu();keyboard();},()->{closeMenu();specialKeys();},
            ()->{closeMenu();disk(true);},()->{closeMenu();launchConfigured();},
            ()->{closeMenu();confirmExit();});
        frame.addView(menu,new FrameLayout.LayoutParams(-1,-1));
    }
    private void closeMenu(){
        if(menu!=null){frame.removeView(menu);menu=null;release();}
    }
    private void launchConfigured(){
        final PcWindowsLaunchRecipe recipe;
        try{recipe=store.launchRecipe(entry);}catch(Exception e){status.setText("Configured executable could not be read");return;}
        if(recipe==null){status.setText("Configure the installed EXE in CARTMOD first");return;}
        new android.app.AlertDialog.Builder(this).setTitle("Launch installed EXE")
            .setMessage("In the running Windows guest, open Start > Run and ensure its input field is empty and focused. Then submit the saved path: "+recipe.getExecutable())
            .setNegativeButton("Cancel",null).setPositiveButton("Run dialog is ready",(d,w)->scheduleKeys("\""+recipe.getExecutable()+"\"\n")).show();
    }
    private void confirmExit(){
        new android.app.AlertDialog.Builder(this)
            .setMessage("Exit only after guest shutdown or writes are complete. Forced exit is not a verified clean shutdown.")
            .setNegativeButton("Cancel",null).setPositiveButton("Return to Shelf",(d,w)->{release();setResult(RESULT_OK,new Intent().putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME,game).putExtra(BaseGameActivity.PLAY_GAME_RESULT_LEANBACK,getIntent().getBooleanExtra("cartdeck.leanback",false)).putExtra(BaseGameActivity.PLAY_GAME_RESULT_SESSION_DURATION,android.os.SystemClock.elapsedRealtime()-startTime));finish();}).show();
    }
    @Override public void onBackPressed(){if(menu!=null)closeMenu();else openMenu();}
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
        String value=text.toLowerCase(java.util.Locale.ROOT);
        if(value.length()>244)return;
        for(int i=0;i<value.length();i++){
            final char c=value.charAt(i);
            final int code=c=='\n'?13:c==':'?';':c=='_'?'-':c=='~'?96:c=='('? '9':c==')'?'0':c=='"'?39:c;
            final boolean shift=c==':'||c=='_'||c=='~'||c=='('||c==')'||c=='"';
            if(!(c>='a'&&c<='z')&&!(c>='0'&&c<='9')&&" .\\/-_:\n~()\"".indexOf(c)<0){release();status.setText("Unsupported guest text");return;}
            view.postDelayed(()->{
                if(active&&menu==null&&hasWindowFocus()&&generation==inputGeneration){
                    if(shift)view.sendKeyEvent(0,KEY+304,0);
                    view.sendKeyEvent(0,KEY+code,0);
                    view.postDelayed(()->{if(active&&generation==inputGeneration){view.sendKeyEvent(1,KEY+code,0);if(shift)view.sendKeyEvent(1,KEY+304,0);}},70);
                }
            },300+i*140L);
        }
        status.setText("Guest keyboard submission scheduled; application launch must be confirmed in Windows.");
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
        if(menu!=null)return super.dispatchKeyEvent(e);
        if(active&&(e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_MODE||e.getKeyCode()==KeyEvent.KEYCODE_MENU)){if(e.getAction()==1)openMenu();return true;}
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
        if(menu!=null)return super.dispatchGenericMotionEvent(e);
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
    @Override protected void onDestroy(){
        release();super.onDestroy();
        // This process owns the shared game lock via GameProcessInitializer.
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
