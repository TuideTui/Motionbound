package br.edu.motionbound;

import br.edu.motionbound.game.CombatRules;
import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.MeleeSwing;
import br.edu.motionbound.input.PlayerCommand;
import br.edu.motionbound.input.SensorControlSource;
import br.edu.motionbound.input.SensorSample;
import com.badlogic.gdx.math.Vector2;

public final class CombatRulesTest {
    public static void main(String[] args) {
        for(int angle=0;angle<360;angle+=45) {
            Vector2 aim=new Vector2(1,0).rotateDeg(angle);
            require(CombatRules.inArc(aim.x*20,aim.y*20,aim),"Hit in direction "+angle);
            require(!CombatRules.inArc(-aim.x*20,-aim.y*20,aim),"No hit behind "+angle);
            require(!CombatRules.inArc(aim.x*40,aim.y*40,aim),"No remote damage "+angle);
        }
        DungeonMap map=new DungeonMap();
        require(CombatRules.clearPath(map,120,136,140,150),"Open diagonal path");
        require(!CombatRules.clearPath(map,280,200,370,200),"Walls block melee");
        MeleeSwing swing=new MeleeSwing();Vector2 aim=new Vector2(1,1).nor();
        swing.begin(aim);aim.set(-1,0);
        require(swing.direction.x>0&&swing.direction.y>0,"Direction locked at start");
        require(!swing.advance(.1f)&&swing.pose()==3,"No damage during preparation");
        require(swing.advance(.07f)&&swing.pose()==4,"One impact at strike");
        require(!swing.advance(.05f)&&!swing.advance(.3f)&&!swing.active(),"No repeated damage");
        swing.begin(new Vector2(0,-1));require(swing.advance(1),"Impact survives a long step");
        require(!swing.advance(1),"Long step cannot hit twice");
        require(MotionboundGame.shouldDropHeart(0)&&MotionboundGame.shouldDropHeart(.299f)
                &&!MotionboundGame.shouldDropHeart(.30f)&&!MotionboundGame.shouldDropHeart(.99f),"30 percent threshold");

        SensorControlSource sensors=new SensorControlSource();
        require(!sensors.read(.02f).attack,"No device means no action");
        sensors.accept(new SensorSample(0,0,0));sensors.read(.02f);
        sensors.accept(new SensorSample(.05f,-.05f,.65f));
        PlayerCommand first=sensors.read(.02f);
        require(first.attack&&first.assistedAim&&!first.cursorAim&&first.direction.isZero(),"Pressure edge and deadzone");
        for(int i=0;i<30;i++){sensors.accept(new SensorSample(0,0,.65f));require(!sensors.read(.02f).attack,"Holding never repeats");}
        sensors.accept(new SensorSample(0,0,.40f));sensors.read(.02f);
        sensors.accept(new SensorSample(0,0,.65f));require(!sensors.read(.02f).attack,"Hysteresis requires release");
        sensors.accept(new SensorSample(0,0,.1f));sensors.read(.02f);
        sensors.accept(new SensorSample(1,1,.65f));require(sensors.read(.02f).attack,"Release arms next attack");
        PlayerCommand lost=sensors.read(.5f);
        require(!lost.attack&&lost.direction.isZero()&&!sensors.connected(),"Disconnect immediately stops commands");
        sensors.accept(new SensorSample(0,0,.7f));require(!sensors.read(.02f).attack,"Reconnect while squeezed is safe");
        sensors.accept(new SensorSample(1,1,0));
        for(int i=0;i<8;i++){sensors.accept(new SensorSample(1,1,0));first=sensors.read(.02f);}
        require(first.direction.x>0&&first.direction.y>0&&first.direction.len()<=1.001f,"Normalized diagonal movement");
        require(sensors.pressureAccuracy()>90,"Measured time in target band");
        float accuracy=sensors.pressureAccuracy();
        sensors.accept(new SensorSample(0,0,1));sensors.read(.02f);
        require(sensors.pressureAccuracy()<accuracy,"Excess pressure is outside the band");
        try {new SensorSample(Float.NaN,0,0);throw new AssertionError("NaN accepted");}
        catch(IllegalArgumentException expected){ }
        System.out.println("PASS: eight directions, range, occlusion, impact timing, drop threshold and sensor pipeline");
    }
    private static void require(boolean valid,String label){if(!valid)throw new AssertionError(label);}
}
