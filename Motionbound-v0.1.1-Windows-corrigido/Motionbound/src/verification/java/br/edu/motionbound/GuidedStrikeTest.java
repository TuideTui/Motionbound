package br.edu.motionbound;

import br.edu.motionbound.game.*;
import com.badlogic.gdx.math.Vector2;

final class GuidedStrikeTest {
    static void verify() {
        DungeonMap map=new DungeonMap();Vector2 position=new Vector2(),aim=new Vector2(1,0);
        GuidedStrike strike=new GuidedStrike();
        for(int angle=0;angle<360;angle+=45) {
            Vector2 delta=new Vector2(70,0).rotateDeg(angle);
            require(!strike.begin(delta.x,delta.y,aim),"Far targets approach first");
            require(strike.advance(map,192,136,1,position),"Bounded approach produces one strike");
            require(Math.abs(position.dst(192,136)-52)<.01f,"Stops before the target in all eight directions");
            require(!strike.advance(map,position.x,position.y,1,position),"No second strike while awaiting impact");
            strike.impact();require(!strike.active(),"Forward attack has no extra retreat");
        }
        require(strike.begin(CombatRules.REACH,0,aim),"Close range includes the boundary");
        require(!strike.moving(),"Close attack starts without a forward dash");
        strike.impact();require(strike.phase==GuidedStrike.RETREAT,"Retreat follows impact");
        strike.advance(map,192,136,1,position);
        require(Math.abs(position.x-174)<.01f&&!strike.active(),"Retreat is exactly 18 units");
        strike.begin(80,0,aim);strike.advance(map,280,200,1,position);
        require(position.x<304&&map.canStand(position.x,position.y,5),"Long frames cannot tunnel through a wall");
        strike.begin(10,0,aim);strike.impact();strike.advance(map,54,136,1,position);
        require(position.x>=53&&map.canStand(position.x,position.y,5),"Retreat stops at the wall");
        strike.reset();strike.validate();
        strike.remaining=Float.NaN;
        try{strike.validate();throw new AssertionError("Invalid state accepted");}catch(IllegalArgumentException expected) { }
        System.out.println("PASS: guided strike geometry, timing, retreat and wall collision");
    }
    private static void require(boolean valid,String message){if(!valid)throw new AssertionError(message);}
}
