package br.edu.motionbound;
import br.edu.motionbound.input.EdgeController;
public final class EdgeUsbCheck {
    public static void main(String[] args)throws Exception {
        try(EdgeController edge=new EdgeController(null)) {
            edge.start();long end=System.nanoTime()+10_000_000_000L;boolean found=false;
            while(System.nanoTime()<end){edge.update(System.nanoTime());if(edge.connected()){found=true;break;}Thread.sleep(20);}
            System.out.println(edge.status());
            if(!found)throw new AssertionError("No healthy Motionbound USB stream");
            for(int i=0;i<50;i++){edge.update(System.nanoTime());if(!edge.connected())throw new AssertionError("Lost stream");Thread.sleep(20);}
            System.out.printf(java.util.Locale.ROOT,"PASS: continuous Edge stream, acceleration X=%+.3f Y=%+.3f%n",edge.tiltX(),edge.tiltY());
        }
    }
}
