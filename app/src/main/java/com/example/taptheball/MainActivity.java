package com.example.taptheball;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.MotionEvent;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(new GameView());
    }

    class GameView extends View {
        Paint p = new Paint(3);
        Random random = new Random();
        Handler handler = new Handler();
        float x=200, y=300, radius=55;
        int score=0, seconds=30;
        boolean started=false, finished=false;
        long lastTick=0;

        GameView() { super(MainActivity.this); setFocusable(true); }

        void moveBall() {
            float pad=radius+12;
            x = pad + random.nextFloat()*Math.max(1,getWidth()-2*pad);
            y = 150 + random.nextFloat()*Math.max(1,getHeight()-220);
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(Color.rgb(245,247,251));
            p.setColor(Color.rgb(24,38,65)); p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); p.setTextSize(30);
            c.drawText("TAP THE BALL", 28, 55, p);
            p.setTextSize(22); p.setColor(Color.DKGRAY);
            c.drawText("Score: "+score, 28, 95, p);
            c.drawText("Time: "+seconds+"s", Math.max(28,getWidth()-145),95,p);
            if (!started) {
                p.setTextSize(23); p.setColor(Color.rgb(60,70,90));
                c.drawText("Tap the ball to start!", 28, getHeight()/2f+100,p);
            } else if (finished) {
                p.setTextSize(30); p.setColor(Color.rgb(24,38,65));
                c.drawText("Time's up!",28,getHeight()/2f,p);
                p.setTextSize(23); c.drawText("Final score: "+score,28,getHeight()/2f+42,p);
                c.drawText("Tap anywhere to play again",28,getHeight()/2f+84,p);
            } else {
                p.setColor(Color.rgb(35,135,245)); c.drawCircle(x,y,radius,p);
                p.setColor(Color.WHITE); p.setTextSize(20); p.setTextAlign(Paint.Align.CENTER);
                c.drawText("TAP",x,y+7,p); p.setTextAlign(Paint.Align.LEFT);
            }
        }

        void startGame() {
            score=0; seconds=30; started=true; finished=false; lastTick=System.currentTimeMillis(); moveBall();
            handler.removeCallbacks(tick); handler.postDelayed(tick,1000);
        }
        Runnable tick = new Runnable() { public void run() {
            if (!started || finished) return;
            seconds--;
            if (seconds<=0) { seconds=0; finished=true; }
            invalidate();
            if (!finished) handler.postDelayed(this,1000);
        }};

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction()!=MotionEvent.ACTION_DOWN) return true;
            if (!started || finished) { startGame(); return true; }
            float dx=e.getX()-x, dy=e.getY()-y;
            if (dx*dx+dy*dy <= radius*radius) { score++; moveBall(); }
            invalidate(); return true;
        }
    }
}
