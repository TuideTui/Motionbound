package br.edu.motionbound;

import br.edu.motionbound.input.TiltSprint;

public final class TiltSprintTest {
    private static void require(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    private static void hold(TiltSprint sprint,float x,float y,int frames) {for(int i=0;i<frames;i++)sprint.update(x,y,.02f);}
    public static void verify() {
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
}
