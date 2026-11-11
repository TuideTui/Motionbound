package br.edu.motionbound;

import br.edu.motionbound.input.TiltSprint;

public final class TiltSprintTest {
    private static void require(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    private static void hold(TiltSprint sprint,float x,float y,int frames) {for(int i=0;i<frames;i++)sprint.update(x,y,.02f);}
    public static void verify() {
        verifyConsistency();
        TiltSprint sprint=new TiltSprint();
        hold(sprint,.59f,0,500);require(sprint.progress()==0,"A gentle tilt never begins charging, even after ten seconds");
        hold(sprint,.8f,0,199);require(!sprint.active()&&sprint.progress()>.99f,"No sprint before four seconds");
        require(sprint.update(.8f,0,.02f),"Sprint starts at four seconds");
        require(sprint.update(.8f,.8f,.02f)&&sprint.update(.8f,-.8f,.02f),"Both diagonals preserve the rightward sprint");
        require(sprint.update(.41f,0,.02f),"Release hysteresis prevents flicker below the start threshold");
        require(!sprint.update(.39f,0,.02f)&&sprint.progress()==0,"Easing below forty percent returns to walking");
        sprint.update(.61f,0,.02f);hold(sprint,.5f,0,199);
        require(sprint.active(),"The middle band preserves an already-started charge");
        require(!sprint.update(-.8f,0,.02f)&&sprint.progress()<.01f,"Opposite direction immediately stops and starts a fresh charge");
        hold(sprint,-.8f,0,199);require(sprint.active(),"Leftward motion can sprint after its own four seconds");
        require(!sprint.update(0,.8f,.02f)&&sprint.progress()<.01f,"Turning to another principal direction restarts the counter");
        hold(sprint,0,.8f,199);require(sprint.active(),"Upward sprint");
        require(sprint.update(-.8f,.8f,.02f)&&sprint.update(.8f,.8f,.02f),"Both upward diagonals keep the upward sprint");
        sprint.reset();hold(sprint,0,-.8f,200);require(sprint.active(),"Downward sprint");
        require(sprint.update(.8f,-.8f,.02f)&&sprint.update(-.8f,-.8f,.02f),"Both downward diagonals keep the downward sprint");
        sprint.exhaust();hold(sprint,-.8f,-.8f,500);
        require(sprint.needsRelease()&&!sprint.active()&&sprint.progress()==0,"Exhaustion cannot pulse between running and recovering");
        sprint.update(.2f,0,.02f);require(!sprint.needsRelease(),"Relaxing re-arms the next sprint");
        hold(sprint,.8f,0,199);require(!sprint.active(),"Re-armed sprint needs four new seconds");
        sprint.update(.8f,0,.02f);require(sprint.active(),"Re-armed sprint completes");
        sprint.reset();require(sprint.progress()==0&&!sprint.active(),"Pause/calibration/stage reset clears running intent");
        hold(sprint,.8f,0,100);sprint.update(0,0,.02f);require(sprint.progress()==0,"Neutral position clears partial intent");
        hold(sprint,.8f,0,100);sprint.update(Float.NaN,0,.02f);require(sprint.progress()==0,"Invalid input cannot leave a sprint latched");
        System.out.println("PASS: four-second tilt sprint, light tilt, direction families, hysteresis, exhaustion and resets");
    }
    private static void verifyConsistency() {
        TiltSprint.Consistency metric=new TiltSprint.Consistency();
        require(metric.score(1)==-1,"No data is N/D");
        for(int i=0;i<500;i++)metric.sample(.1f,0,.02,.35f,true);
        require(metric.seconds[0]==0,"Ordinary walking is not a motor exercise");
        for(int phase=1;phase<=5;phase++) {
            metric.beginStage(phase);
            for(int i=0;i<200;i++)metric.sample(.35f,0,.02,.35f,true);
            require(metric.score(phase)==100,"The same four-second hold scores equally in every phase");
            double before=metric.seconds[phase-1];
            for(int i=0;i<200;i++)metric.sample(.35f,0,.02,.35f,true);
            require(Math.abs(metric.seconds[phase-1]-before)<.000001,"Continuous running does not create extra windows");
        }
        metric.beginStage(2);
        for(int i=0;i<100;i++)metric.sample(.35f,0,.02,.35f,true);
        require(metric.score(2)==-1,"Less than four valid seconds is N/D");
        for(int i=0;i<100;i++)metric.sample(.17f,0,.02,.35f,true);
        require(metric.score(2)==50,"Wavering during a canceled hold still lowers the score");
        metric.beginStage(2);
        for(int i=0;i<100;i++)metric.sample(.35f,0,.01,.35f,true);
        for(int i=0;i<100;i++)metric.sample(.17f,0,.03,.35f,true);
        require(metric.score(2)==25,"The score is weighted by sensor time, not by sample count");
        metric.beginStage(3);
        for(int i=0;i<100;i++)metric.sample(.35f,0,.02,.35f,true);
        for(int i=0;i<100;i++)metric.sample(-.35f,0,.02,.35f,true);
        require(metric.score(3)==50,"Opposite direction is outside the original band");
        metric.beginStage(4);
        for(int i=0;i<100;i++)metric.sample(.35f,0,.02,.35f,true);
        double before=metric.seconds[3];metric.interrupt();
        metric.sample(Float.NaN,0,.02,.35f,true);metric.sample(.35f,0,5,.35f,true);
        require(metric.seconds[3]==before,"Invalid data and disconnected intervals never count");
        for(int i=0;i<100;i++)metric.sample(.35f,0,.02,.35f,true);
        require(metric.score(4)==100,"Interrupting a session preserves measured totals, excluding the break");
        metric.beginStage(5);
        for(int i=0;i<200;i++)metric.sample(.17f,0,.02,.23f,true);
        require(metric.score(5)==0,"Changing control sensitivity does not lower the campaign's scoring threshold");
        metric.validate();metric.clear(.35f);require(metric.score(1)==-1&&metric.score(5)==-1,"New campaigns clear history");
    }
}
