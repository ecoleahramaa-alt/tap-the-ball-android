package com.example.taptheball;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(9, 13, 31));
        w.setNavigationBarColor(Color.rgb(9, 13, 31));
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new ArcadeView(this));
    }

    static class ArcadeView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random random = new Random();
        final SharedPreferences prefs;
        final ArrayList<Particle> particles = new ArrayList<>();
        final ArrayList<Star> stars = new ArrayList<>();
        int screen = 0; // 0 menu, 1 playing, 2 result
        int score = 0, best, seconds = 30, combo = 0, maxCombo = 0;
        long lastSecond = 0, lastFrame = 0;
        float targetX, targetY, targetR = 38, bombX = -200, bombY = -200, bombR = 28;
        boolean bombVisible = false;
        float pulse = 0;
        String message = "READY?";
        final int bg = Color.rgb(9,13,31), cyan = Color.rgb(63,235,222), pink = Color.rgb(255,80,153), gold = Color.rgb(255,208,76);
        float downX, downY;

        ArcadeView(Context c) {
            super(c);
            prefs = c.getSharedPreferences("neon_tap_save", MODE_PRIVATE);
            best = prefs.getInt("best", 0);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            for (int i=0;i<55;i++) stars.add(new Star(random.nextFloat(), random.nextFloat(), 1+random.nextFloat()*2));
            setFocusable(true);
        }

        void text(Canvas c, String s, float x, float y, float size, int color, boolean bold, Paint.Align align) {
            p.setShader(null); p.setColor(color); p.setTextSize(size); p.setTextAlign(align);
            p.setTypeface(Typeface.create("sans-serif" + (bold ? "-black" : ""), bold ? Typeface.BOLD : Typeface.NORMAL));
            p.setStyle(Paint.Style.FILL); p.setShadowLayer(0,0,0,Color.TRANSPARENT); c.drawText(s,x,y,p);
        }

        void rounded(Canvas c,float l,float t,float r,float b,float rad,int color) {
            p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); p.clearShadowLayer();
            c.drawRoundRect(l,t,r,b,rad,rad,p);
        }

        void logo(Canvas c, float cx, float cy, float scale) {
            p.setShader(null); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4*scale); p.setColor(cyan);
            p.setShadowLayer(18*scale,0,0,cyan); c.drawCircle(cx,cy,31*scale,p); p.clearShadowLayer();
            p.setStyle(Paint.Style.FILL); p.setColor(pink); c.drawCircle(cx+9*scale,cy-8*scale,12*scale,p);
            p.setColor(gold); c.drawCircle(cx-10*scale,cy+10*scale,7*scale,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2*scale); p.setColor(Color.WHITE);
            c.drawArc(cx-21*scale,cy-21*scale,cx+21*scale,cy+21*scale,210,105,false,p);
            p.setStyle(Paint.Style.FILL);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w=getWidth(), h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h, new int[]{Color.rgb(10,15,38),Color.rgb(24,18,53),Color.rgb(8,22,40)},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            for (Star s:stars) { p.setColor(Color.argb(s.a,170,210,255)); c.drawCircle(s.x*w,s.y*h,s.r,p); }
            pulse += .045f;
            if (screen==0) drawMenu(c,w,h);
            else if (screen==1) drawGame(c,w,h);
            else drawResult(c,w,h);
            drawParticles(c);
            postInvalidateDelayed(32);
        }

        void drawMenu(Canvas c,int w,int h) {
            float cx=w/2f;
            logo(c,cx,h*.19f,1.35f);
            text(c,"NEON",cx,h*.19f+82,46,Color.WHITE,true,Paint.Align.CENTER);
            text(c,"TAP",cx,h*.19f+126,46,cyan,true,Paint.Align.CENTER);
            text(c,"ARCADE • QUICK REFLEX CHALLENGE",cx,h*.19f+158,11,Color.rgb(170,184,220),true,Paint.Align.CENTER);
            float cardTop=h*.43f;
            rounded(c,24,cardTop,w-24,cardTop+116,22,Color.argb(185,26,34,67));
            text(c,"30 SECONDS",cx,cardTop+35,17,gold,true,Paint.Align.CENTER);
            text(c,"Tap cyan targets. Avoid pink bombs!",cx,cardTop+64,14,Color.WHITE,false,Paint.Align.CENTER);
            text(c,"Build combos for bonus points",cx,cardTop+88,13,Color.rgb(173,194,222),false,Paint.Align.CENTER);
            float by=h*.68f;
            rounded(c,34,by,w-34,by+62,19,cyan);
            text(c,"PLAY NOW  ▶",cx,by+39,19,bg,true,Paint.Align.CENTER);
            text(c,"BEST SCORE   "+best,cx,by+105,15,Color.WHITE,true,Paint.Align.CENTER);
            text(c,"One tap can change everything.",cx,h-34,12,Color.rgb(143,158,195),false,Paint.Align.CENTER);
        }

        void drawGame(Canvas c,int w,int h) {
            text(c,"NEON TAP",22,36,16,Color.WHITE,true,Paint.Align.LEFT);
            rounded(c,w-102,13,w-20,47,16,Color.argb(200,38,44,78));
            text(c,seconds+"s",w-61,37,19,seconds<=7?pink:gold,true,Paint.Align.CENTER);
            text(c,"SCORE",22,73,10,Color.rgb(156,174,211),true,Paint.Align.LEFT);
            text(c,String.valueOf(score),22,112,37,Color.WHITE,true,Paint.Align.LEFT);
            text(c,"COMBO x"+combo, w-22,76,13,cyan,true,Paint.Align.RIGHT);
            text(c,"Tap the glowing targets",w/2f,h-35,13,Color.rgb(168,183,216),false,Paint.Align.CENTER);
            // Target glow
            p.setShader(new RadialGradient(targetX,targetY,targetR*1.8f,new int[]{Color.argb(105,63,235,222),Color.argb(0,63,235,222)},null,Shader.TileMode.CLAMP));
            c.drawCircle(targetX,targetY,targetR*1.8f,p); p.setShader(null);
            p.setColor(cyan); p.setStyle(Paint.Style.FILL); c.drawCircle(targetX,targetY,targetR,p);
            p.setColor(Color.WHITE); c.drawCircle(targetX,targetY,targetR*.66f,p);
            p.setColor(cyan); c.drawCircle(targetX,targetY,targetR*.48f,p);
            p.setColor(Color.WHITE); c.drawCircle(targetX,targetY,targetR*.13f,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(Color.argb(180,255,255,255));
            c.drawCircle(targetX,targetY,targetR+7+(float)Math.sin(pulse)*3,p); p.setStyle(Paint.Style.FILL);
            if (bombVisible) {
                p.setColor(pink); c.drawCircle(bombX,bombY,bombR,p);
                p.setColor(Color.WHITE); p.setStrokeWidth(4); p.setStyle(Paint.Style.STROKE);
                c.drawLine(bombX-9,bombY-9,bombX+9,bombY+9,p); c.drawLine(bombX+9,bombY-9,bombX-9,bombY+9,p); p.setStyle(Paint.Style.FILL);
            }
        }

        void drawResult(Canvas c,int w,int h) {
            float cx=w/2f;
            logo(c,cx,h*.22f,1.0f);
            text(c,"TIME'S UP!",cx,h*.22f+75,32,Color.WHITE,true,Paint.Align.CENTER);
            text(c,"YOUR SCORE",cx,h*.22f+120,13,Color.rgb(171,188,220),true,Paint.Align.CENTER);
            text(c,String.valueOf(score),cx,h*.22f+178,66,cyan,true,Paint.Align.CENTER);
            if(score>=best && score>0) text(c,"✦ NEW PERSONAL BEST! ✦",cx,h*.22f+215,14,gold,true,Paint.Align.CENTER);
            else text(c,"BEST SCORE  "+best,cx,h*.22f+215,14,gold,true,Paint.Align.CENTER);
            rounded(c,28,h*.52f,w-28,h*.52f+86,18,Color.argb(190,26,34,67));
            text(c,"BEST COMBO",cx-75,h*.52f+33,12,Color.rgb(165,184,220),true,Paint.Align.CENTER);
            text(c,""+maxCombo,cx-75,h*.52f+61,24,Color.WHITE,true,Paint.Align.CENTER);
            text(c,"TARGETS HIT",cx+75,h*.52f+33,12,Color.rgb(165,184,220),true,Paint.Align.CENTER);
            text(c,""+score,cx+75,h*.52f+61,24,Color.WHITE,true,Paint.Align.CENTER);
            float by=h*.68f;
            rounded(c,30,by,w-30,by+60,18,cyan);
            text(c,"PLAY AGAIN  ↻",cx,by+38,18,bg,true,Paint.Align.CENTER);
            text(c,"HOME",cx,h-35,13,Color.rgb(166,184,216),true,Paint.Align.CENTER);
        }

        void moveTarget() {
            float margin=targetR+12, top=145, bottom=getHeight()-90;
            targetX=margin+random.nextFloat()*Math.max(1,getWidth()-margin*2);
            targetY=top+random.nextFloat()*Math.max(1,bottom-top);
            targetR=Math.max(27,38-Math.min(9,score/8f));
            bombVisible=score>=4 && random.nextFloat()<0.48f;
            if(bombVisible) {
                bombX=margin+random.nextFloat()*Math.max(1,getWidth()-margin*2);
                bombY=top+random.nextFloat()*Math.max(1,bottom-top);
                if(Math.hypot(bombX-targetX,bombY-targetY)<targetR+bombR+25) bombVisible=false;
            }
        }

        void start() {
            score=0; seconds=30; combo=0; maxCombo=0; screen=1;
            particles.clear(); lastSecond=System.currentTimeMillis(); moveTarget(); invalidate();
        }

        void end() {
            screen=2; bombVisible=false;
            if(score>best) { best=score; prefs.edit().putInt("best",best).apply(); }
            invalidate();
        }

        void burst(float x,float y,int color) {
            for(int i=0;i<14;i++) {
                double a=random.nextDouble()*Math.PI*2;
                float speed=2+random.nextFloat()*7;
                particles.add(new Particle(x,y,(float)Math.cos(a)*speed,(float)Math.sin(a)*speed,color,22+random.nextInt(15)));
            }
        }

        void drawParticles(Canvas c) {
            for(int i=particles.size()-1;i>=0;i--) {
                Particle q=particles.get(i); q.x+=q.vx; q.y+=q.vy; q.vy+=.12f; q.life--;
                p.setColor(Color.argb(Math.max(0,Math.min(255,q.life*10)),Color.red(q.color),Color.green(q.color),Color.blue(q.color)));
                c.drawCircle(q.x,q.y,Math.max(1,q.life/5f),p);
                if(q.life<=0) particles.remove(i);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_DOWN) return true;
            float x=e.getX(), y=e.getY(); int w=getWidth(), h=getHeight();
            if(screen==0) { start(); return true; }
            if(screen==2) {
                if(y>h*.62f) start(); else screen=0;
                invalidate(); return true;
            }
            if(bombVisible && Math.hypot(x-bombX,y-bombY)<=bombR+12) {
                score=Math.max(0,score-3); combo=0; bombVisible=false; burst(bombX,bombY,pink); message="OOPS"; invalidate(); return true;
            }
            if(Math.hypot(x-targetX,y-targetY)<=targetR+12) {
                combo++; maxCombo=Math.max(maxCombo,combo);
                score += 1 + (combo>=5 ? 2 : combo>=3 ? 1 : 0);
                burst(targetX,targetY,cyan); moveTarget();
            } else {
                combo=0;
            }
            invalidate(); return true;
        }

        @Override public void computeScroll() {
            super.computeScroll();
            if(screen==1) {
                long now=System.currentTimeMillis();
                if(now-lastSecond>=1000) {
                    int dec=(int)((now-lastSecond)/1000);
                    seconds-=dec; lastSecond+=dec*1000L;
                    if(seconds<=0) { seconds=0; end(); }
                }
            }
        }

        static class Star { float x,y,r; int a; Star(float x,float y,float r){this.x=x;this.y=y;this.r=r;this.a=70+(int)(Math.random()*120);} }
        static class Particle { float x,y,vx,vy; int color,life; Particle(float x,float y,float vx,float vy,int color,int life){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.color=color;this.life=life;} }
    }
}
