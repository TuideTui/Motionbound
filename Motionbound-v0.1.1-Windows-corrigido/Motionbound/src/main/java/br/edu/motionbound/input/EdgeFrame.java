package br.edu.motionbound.input;

/** Versioned, bounded USB protocol. Boot messages and diagnostic text are ignored. */
public record EdgeFrame(long boot,long sequence,float x,float y,float z,int buttons,long attacks,long magic,int flags) {
    public static EdgeFrame parse(String line) {
        if(line==null||line.length()>180||!line.startsWith("MB1;"))return null;
        try {
            String[] p=line.split(";",-1);if(p.length!=10)return null;
            long boot=unsigned(p[1]),seq=unsigned(p[2]),attack=unsigned(p[7]),magic=unsigned(p[8]);
            float x=Float.parseFloat(p[3]),y=Float.parseFloat(p[4]),z=Float.parseFloat(p[5]);
            int buttons=Integer.parseInt(p[6]),flags=Integer.parseInt(p[9]);
            if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z)
                    ||Math.abs(x)>2.1f||Math.abs(y)>2.1f||Math.abs(z)>2.1f||buttons<0||buttons>15||flags<0||flags>7)return null;
            return new EdgeFrame(boot,seq,x,y,z,buttons,attack,magic,flags);
        }catch(IllegalArgumentException e){return null;}
    }
    private static long unsigned(String value){long n=Long.parseLong(value);if(n<0||n>0xffffffffL)throw new IllegalArgumentException();return n;}
    public static long distance(long next,long previous){return(next-previous)&0xffffffffL;}
}
