package br.edu.motionbound.ui;

import br.edu.motionbound.game.PotionEffect;
import br.edu.motionbound.game.StatusEffects;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/** Drawn, four-frame pixel flames and smoke; poison keeps its approved bubble animation. */
public final class ScreenStatusEffects implements Disposable {
    private static final PotionEffect[] TYPES=PotionEffect.values();
    private static final Color POISON=Color.valueOf("69cf50"),LIME=Color.valueOf("d5ff98"),SILVER=Color.valueOf("e8f1f7");
    private static final Color[] FIRE={Color.valueOf("602416"),Color.valueOf("bd4320"),Color.valueOf("e58e43"),Color.valueOf("f0d764"),Color.valueOf("fff0a1")};
    private static final Color[] SMOKE={Color.valueOf("555563"),Color.valueOf("7b7e89"),Color.valueOf("a3a8b1"),Color.valueOf("c4c7cc")};
    private final Texture pixel,glow;
    private final Texture[] flames=new Texture[4],clouds=new Texture[4];
    private final float[] strength=new float[TYPES.length],phase=new float[TYPES.length];
    private float width,height,band,density;
    private SpriteBatch batch;

    public ScreenStatusEffects(Texture pixel,Texture glow) {
        this.pixel=pixel;this.glow=glow;
        for(int i=0;i<4;i++){flames[i]=flameFrame(i);clouds[i]=smokeFrame(i);}
    }
    public void update(StatusEffects effects,float delta) {
        for(PotionEffect type:TYPES) {
            int i=type.ordinal();
            if(type==PotionEffect.RIFT){strength[i]=phase[i]=0;continue;}
            float remaining=type==PotionEffect.SLOW?effects.slowRemaining:effects.damageEffect==type?effects.damageRemaining():0;
            float target=MathUtils.clamp(remaining/.5f,0,1),speed=target>strength[i]?6:3;
            strength[i]+=MathUtils.clamp(target-strength[i],-delta*speed,delta*speed);
            if(strength[i]>0)phase[i]=(phase[i]+delta)%120;else phase[i]=0;
        }
    }
    public void clear(){java.util.Arrays.fill(strength,0);java.util.Arrays.fill(phase,0);}
    public void draw(SpriteBatch batch,float width,float height,float density) {
        this.batch=batch;this.width=width;this.height=height;this.density=density;
        band=MathUtils.clamp(Math.min(width,height)*.125f,64,96);
        for(PotionEffect type:TYPES) {
            if(type==PotionEffect.RIFT||strength[type.ordinal()]<=0)continue;
            float time=phase[type.ordinal()],opacity=strength[type.ordinal()];
            int spacing=type==PotionEffect.FIRE?90:type==PotionEffect.SLOW?60:56;
            int count=MathUtils.clamp((int)(height/spacing),8,24);
            for(int side=0;side<2;side++)for(int n=0;n<count;n++) {
                int seed=n+side*97+type.ordinal()*211;
                float life=type==PotionEffect.FIRE?3.8f+noise(seed+11)*2.4f:type==PotionEffect.SLOW?6+noise(seed+11)*4:3+noise(seed+11)*1.8f;
                float age=fraction(time/life+noise(seed+5))*life,t=age/life,s=noise(seed+31)*MathUtils.PI2;
                float alpha=opacity*MathUtils.clamp(age/.16f,0,1)*MathUtils.clamp((life-age)/.35f,0,1);
                float depth,y;
                if(type==PotionEffect.POISON) {
                    depth=band*.5f+MathUtils.sin(age*1.8f+s)*band*.17f+MathUtils.sin(age*.6f+s)*4;
                    y=-18+(height+36)*t;
                } else if(type==PotionEffect.FIRE) {
                    depth=band*(.28f+noise(seed+47)*.4f)+MathUtils.sin(age*.9f+s)*10;
                    y=-65+(height+130)*t;
                } else {
                    depth=band*.23f+MathUtils.sin(age*.45f+s)*band*.12f;
                    y=-90+(height+180)*t;
                }
                float x=side==0?depth:width-depth;
                if(type==PotionEffect.FIRE) {
                    int frame=(int)(time*9+seed*.7f)%4;
                    float cell=3+(seed&1),w=14*cell,h=24*cell;
                    sprite(flames[frame],side,x-w/2,y-h/2,w,h,Color.WHITE,alpha*.95f);
                    // Detached red/orange pixel embers accompany the animated drawn flames.
                    float ey=y+h*.6f+12,ex=x+MathUtils.sin(age*2+s)*9,size=3+(seed%3);
                    sprite(pixel,side,ex,ey,size,size,FIRE[1],alpha);
                    sprite(pixel,side,ex+9,ey+18,2,2,FIRE[3],alpha*.8f);
                } else if(type==PotionEffect.SLOW) {
                    int frame=(int)(time*5+seed*.4f)%4;
                    float cell=3+(seed&1),w=16*cell,h=20*cell;
                    sprite(clouds[frame],side,x-w/2,y-h/2,w,h,Color.WHITE,alpha*.43f);
                } else bubble(side,seed,t,x,y,alpha);
            }
        }
        batch.setColor(Color.WHITE);this.batch=null;
    }
    private void bubble(int side,int seed,float t,float x,float y,float alpha) {
        float radius=(3+noise(seed+71)*4)*1.9f;
        if(t>.82f)radius*=1+(t-.82f)*1.8f;
        int src=batch.getBlendSrcFunc(),dst=batch.getBlendDstFunc();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE);
        sprite(glow,side,x-24,y-24,48,48,POISON,alpha*.28f);batch.setBlendFunction(src,dst);
        sprite(pixel,side,x-radius+2,y-radius,2*radius-4,2,POISON,alpha*.85f);
        sprite(pixel,side,x-radius+2,y+radius-2,2*radius-4,2,LIME,alpha*.9f);
        sprite(pixel,side,x-radius,y-radius+2,2,2*radius-4,POISON,alpha*.85f);
        sprite(pixel,side,x+radius-2,y-radius+2,2,2*radius-4,LIME,alpha*.9f);
        sprite(pixel,side,x-radius+2,y+radius-3,3,2,SILVER,alpha*.85f);
        if(t>.82f)for(int n=0;n<4;n++) {
            float angle=n*90+seed,r=radius+3+(t-.82f)*30;
            sprite(pixel,side,x+MathUtils.cosDeg(angle)*r,y+MathUtils.sinDeg(angle)*r,2,2,LIME,alpha*(1-t)*4);
        }
        sprite(pixel,side,x+MathUtils.sin(seed)*5,y-12,1.5f,1.5f,POISON,alpha*.6f);
    }
    private void sprite(Texture texture,int side,float x,float y,float w,float h,Color color,float alpha) {
        float x0=Math.max(side==0?0:width-band,x),x1=Math.min(side==0?band:width,x+w);
        float y0=Math.max(0,y),y1=Math.min(height,y+h);
        if(x0>=x1||y0>=y1||alpha<=0)return;
        float u0=(x0-x)/w,u1=(x1-x)/w,v0=1-(y1-y)/h,v1=1-(y0-y)/h;
        float left=Math.round(x0*density)/density,bottom=Math.round(y0*density)/density;
        float right=Math.round(x1*density)/density,top=Math.round(y1*density)/density;
        batch.setColor(color.r,color.g,color.b,MathUtils.clamp(alpha,0,1));
        batch.draw(texture,left,bottom,right-left,top-bottom,u0,v0,u1,v1);
    }
    private static Texture flameFrame(int frame) {
        int[] left={7,7,6,6,5,4,4,5,5,4,3,3,2,2,3,3,2,2,3,3,4,4};
        int[] right={8,8,8,9,9,8,7,8,10,11,11,10,10,11,12,12,11,11,10,10,9,9};
        Pixmap image=new Pixmap(14,24,Pixmap.Format.RGBA8888);
        for(int y=0;y<left.length;y++) {
            int sway=Math.round((frame==0?-2:frame==2?2:0)*(22-y)/22f);
            int l=MathUtils.clamp(left[y]+sway,0,13),r=MathUtils.clamp(right[y]+sway,0,13);
            for(int x=l;x<=r;x++) {
                int shade=y>19?0:1;
                if(y>=4&&x>l&&x<r)shade=2;
                if(y>=11&&y<=20&&x>=l+2&&x<=Math.min(r-1,l+4+(frame%2)))shade=3;
                if(y>=15&&y<=19&&x==l+3)shade=4;
                // The clipped sprite UVs run bottom-up: keep the narrow flame tip above its base.
                image.setColor(FIRE[shade]);image.drawPixel(x,22-y);
            }
        }
        return texture(image);
    }
    private static Texture smokeFrame(int frame) {
        int[] left={6,4,3,2,2,1,1,0,1,1,2,2,1,2,3,3,4,5,6};
        int[] right={8,10,12,13,14,14,13,14,15,14,14,13,12,12,13,12,11,9,8};
        Pixmap image=new Pixmap(16,20,Pixmap.Format.RGBA8888);
        for(int y=0;y<left.length;y++) {
            int ripple=(y/4+frame)%3==0?1:0;
            int l=left[y]+ripple,r=right[y]-(frame%2==0&&y<6?1:0);
            for(int x=l;x<=r;x++) {
                int shade=x==l||x==r||y>15?0:1;
                if(x>=l+2&&x<=r-2&&y<14)shade=2;
                if((y>=3&&y<=6||y>=10&&y<=12)&&x>=l+2&&x<=l+4)shade=3;
                image.setColor(SMOKE[shade]);image.drawPixel(x,y);
            }
        }
        return texture(image);
    }
    private static Texture texture(Pixmap image) {
        Texture result=new Texture(image);image.dispose();result.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);return result;
    }
    @Override public void dispose(){for(Texture t:flames)t.dispose();for(Texture t:clouds)t.dispose();}
    private static float fraction(float value){return value-(float)Math.floor(value);}
    private static float noise(int seed) {
        int value=seed*0x45d9f3b;value=(value^(value>>>16))*0x45d9f3b;value^=value>>>16;
        return (value&0x7fffffff)/(float)Integer.MAX_VALUE;
    }
}
