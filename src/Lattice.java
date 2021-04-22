import javax.swing.*;
import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Random;

public class Lattice extends JPanel {
    double[][][] lattice; // f
    double[][][] latticeStar; // f*
    double[][][] eqDis; // f^eq
    double[][] macroDense; // p
    double[][][] macroVel; // u

    boolean[][] isPart;

    boolean[][] isWall;
    short[][] zouHe; // 0 is none, 1 is pressure, 2 is velocity, 3 is both
    short[][] zouHeOr; // 0 is top, 1 is left, 2 is bottom, 3 is right
    double[][] zhPres;
    double[][][] zhVel;

    int width;
    int height;

    int c = 1; //lattice speed
    double t = 5d/6; // relaxation time to equilibrium
    double denseMult = 128;
    double velMult = 128;
    double lim = 1e100;
    double sqrMult = 0.5;
    double partTop = 175;
    double partBot = 75;

    setup curSet;
    color curCol;

    enum setup {
        MoveLid,
        Poiseuille
    }

    enum color {
        partVel,
        denseVel
    }

    private double[][] e = new double[][] {
            {0,0},
            {1,0},
            {0,1},
            {-1,0},
            {0,-1},
            {1,1},
            {-1,1},
            {-1,-1},
            {1,-1}
    };

    private final double[] w = new double[] {
        4d/9, 1d/9,  1d/9,  1d/9,  1d/9,  1d/36, 1d/36, 1d/36, 1d/36
    };

    private double s (int i, double[] u){
        return w[i] * (3*dot(e[i],u)/c + (9d/2)*pow2(dot(e[i],u))/(c*c) - (3d/2)*dot(u,u)/(c*c));
    }

    private final int[] inv = new int[] {
            0, 3, 4, 1, 2, 7, 8, 5, 6
    };

    public Lattice(int width, int height, setup cur, color curColl) {
        this.width = width;
        this.height = height;

        curSet = cur;
        curCol = curColl;

        zouHe = new short[width][height];
        zouHeOr = new short[width][height];
        zhPres = new double[width][height];
        zhVel = new double[width][height][2];

        this.resetWalls();
        this.resetFlow();
    }

    public void resetFlow () {
        lattice = new double[width][height][9];
        latticeStar = new double[width][height][9];
        eqDis = new double[width][height][9];
        macroDense = new double[width][height];
        macroVel = new double[width][height][2];

        isPart = new boolean[width][height];

        if(isWall.length != width || isWall[0].length != height){
            //TODO: add in resizing
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < 9; k++) {
                    lattice[i][j][k] = w[k];
//                    latticeStar[i][j][k] = w[k];
                }

//                lattice[i][j][1] += 1;

//                macroDense[i][j] = 1;
//                macroVel[i][j][0] = 0.41;

                if(i == 0){
                    isWall[i][j] = true;
                } else if(i == width-1){
                    isWall[i][j] = true;
                } else if(j == 0){
                    isWall[i][j] = true;
                } else if (j == height-1){
                    isWall[i][j] = true;
                }
            }
        }

        if(curSet == setup.Poiseuille) {
            for (int i = 2; i < height-2; i++) {
                zouHe[1][i] = 3;
                zouHeOr[1][i] = 1;
                zhVel[1][i][0] = 1d/2;
                zhPres[1][i] = 1;
            }

            for (int i = 2; i < height-2; i++) {
                zouHe[width-2][i] = 1;
                zouHeOr[width-2][i] = 3;
//                lat.zhVel[lat.width-2][i][0] = -1d/7;
                zhPres[width-2][i] = -0;
            }

            stream2();
//        updateMDMVED();
            updateMD();
            updateMV();

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
//                macroDense[i][j] = (1d-0.2)/width * (width-i);
//                macroDense[i][j] = 1.3;
                    macroVel[i][j][0] = 0.1;
//                macroVel[i][j][0] = (0.41-0.21)/width * (width-i) + 0.21;
                }
            }

            updateED();
            collide();
            moveParts();
        }else if (curSet == setup.MoveLid){
            for (int i = 2; i < height-2; i++) {
                zouHe[1][i] = 2;
                zouHeOr[1][i] = 1;
                zhVel[1][i][1] = 1d/10;
            }
        }
    }

    public void resetWalls () {
        isWall = new boolean[width][height];
    }

    private static double dot(double[] vect_A, double[] vect_B) {
        double product = 0;

        // Loop for calculate cot product
        for (int i = 0; i < vect_A.length; i++)
            product = product + vect_A[i] * vect_B[i];
        return product;
    }

    private static double pow2(double a) {
        return a*a;
    }

    public double clamp (double min, double max, double in){
        return Math.max(min, Math.min(max, in));
    }

    public void clampAll (){
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < 9; k++) {
                    lattice[i][j][k] = clamp(-lim, lim, lattice[i][j][k]);
                    latticeStar[i][j][k] = clamp(-lim, lim, latticeStar[i][j][k]);
                    eqDis[i][j][k] = clamp(-lim, lim, eqDis[i][j][k]);
                }
                macroDense[i][j] = clamp(-lim, lim, macroDense[i][j]);
                macroVel[i][j][0] = clamp(-lim, lim, macroVel[i][j][0]);
                macroVel[i][j][1] = clamp(-lim, lim, macroVel[i][j][1]);
            }
        }
    }

    private void stream () {
        latticeStar = new double[width][height][9];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < 9; k++) {
                    int xGo = i+c*(int)e[k][0];
                    int yGo = j+c*(int)e[k][1];
                    int dir = k;

                    if(xGo < 0) {
//                        xGo = Math.abs(xGo);
                        xGo = 0;
                        dir = inv[k];
                    }else if (xGo >= width) {
//                        xGo = width - (xGo - width) - 1;
                        xGo = width-1;
                        dir = inv[k];
                    }

                    if(yGo < 0) {
//                        yGo = Math.abs(yGo);
                        yGo = 0;
                        dir = inv[k];
                    }else if (yGo >= height) {
//                        yGo = height - (yGo - height) - 1;
                        yGo = height-1;
                        dir = inv[k];
                    }

                    latticeStar[xGo][yGo][dir] = lattice[i][j][k];
                }
            }
        }
    }

    private void stream2 () {
        latticeStar = new double[width][height][9];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {

                for (int k = 0; k < 9; k++) {
                    int xLook = i-(int)e[k][0];
                    int yLook = j-(int)e[k][1];

                    if(xLook<0 || xLook >=width || yLook<0 || yLook >=height) {
                        latticeStar[i][j][k] = w[k];
                    }else if(isWall[xLook][yLook]){
//                        System.out.println("i " + i + " j " + j + " k " + k);
//                        System.out.println(latticeStar.length);
//                        System.out.println(latticeStar[0].length);
//                        System.out.println(latticeStar[0][0].length);
//
//                        System.out.println(lattice.length);
//                        System.out.println(lattice[0].length);
//                        System.out.println(lattice[0][0].length);

                        latticeStar[i][j][k] = lattice[i][j][inv[k]];
                    }else {
                        latticeStar[i][j][k] = lattice[xLook][yLook][k];
                    }
                }

                if(zouHe[i][j] != 0){

                    double presh = ((zouHe[i][j] & 1) != 0)? zhPres[i][j] : macroDense[i][j];
                    double[] velo = ((zouHe[i][j] & 2) != 0)? zhVel[i][j] : macroVel[i][j];

                    if(zouHeOr[i][j] == 0){ // top

                    }else if (zouHeOr[i][j] == 1) { // left
                        latticeStar[i][j][1] = latticeStar[i][j][3] + (2d/3) * presh * velo[1];

                        latticeStar[i][j][5] = latticeStar[i][j][7] - (1d/2)*(latticeStar[i][j][2] - latticeStar[i][j][4]) +
                                (1d/6)*presh*velo[0] + (1d/2)*presh*velo[1];

                        latticeStar[i][j][8] = latticeStar[i][j][6] + (1d/2)*(latticeStar[i][j][2] - latticeStar[i][j][4]) +
                                (1d/6)*presh*velo[0] - (1d/2)*presh*velo[1];
                    }else if (zouHeOr[i][j] == 2) { // bottom

                    }else if (zouHeOr[i][j] == 3) { // right
                        latticeStar[i][j][3] = latticeStar[i][j][1] - (2d/3) * presh * velo[1];

                        latticeStar[i][j][7] = latticeStar[i][j][5] - (1d/2)*(latticeStar[i][j][4] - latticeStar[i][j][2]) +
                                (1d/6)*presh*velo[0] + (1d/2)*presh*velo[1];

                        latticeStar[i][j][6] = latticeStar[i][j][8] + (1d/2)*(latticeStar[i][j][4] - latticeStar[i][j][2]) +
                                (1d/6)*presh*velo[0] - (1d/2)*presh*velo[1];

//                        latticeStar[i][j][3] = 0.05;
//                        latticeStar[i][j][7] = 0.05;
//                        latticeStar[i][j][6] = 0.05;
                    }
                }
            }
        }
    }

    private void collide () {
        lattice = new double[width][height][9];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < 9; k++) {
                    lattice[i][j][k] = latticeStar[i][j][k] - (latticeStar[i][j][k]-eqDis[i][j][k])/t;
                }
            }
        }
    }

    private void updateMD () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                macroDense[i][j] = 0;
                for (int k = 0; k < 9; k++) {
                    macroDense[i][j] += latticeStar[i][j][k];
                }
            }
        }
    }

    private void updateMV () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                macroVel[i][j][0] = 0;
                macroVel[i][j][1] = 0;
                for (int k = 0; k < 9; k++) {
                    macroVel[i][j][0] += c*latticeStar[i][j][k]*e[k][0];
                    macroVel[i][j][1] += c*latticeStar[i][j][k]*e[k][1];
                }

                if(macroDense[i][j] == 0){
                    macroVel[i][j][0] = 0;
                    macroVel[i][j][1] = 0;
                }else {
                    macroVel[i][j][0] /= macroDense[i][j];
                    macroVel[i][j][1] /= macroDense[i][j];
                }
            }
        }
    }

    private void updateMDMVED () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                macroDense[i][j] = 0;
                for (int k = 0; k < 9; k++) {
                    macroDense[i][j] += latticeStar[i][j][k];
                }

                macroVel[i][j][0] = 0;
                macroVel[i][j][1] = 0;
                for (int k = 0; k < 9; k++) {
                    macroVel[i][j][0] += c*latticeStar[i][j][k]*e[k][0];
                    macroVel[i][j][1] += c*latticeStar[i][j][k]*e[k][1];
                }

                if(macroDense[i][j] == 0){
                    macroVel[i][j][0] = 0;
                    macroVel[i][j][1] = 0;
                }else {
                    macroVel[i][j][0] /= macroDense[i][j];
                    macroVel[i][j][1] /= macroDense[i][j];
                }

                for (int k = 0; k < 9; k++) {
                    eqDis[i][j][k] = w[k]*macroDense[i][j] + macroDense[i][j]*s(k, macroVel[i][j]);
                }
            }
        }
    }

    private void updateED () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < 9; k++) {
                    eqDis[i][j][k] = w[k]*macroDense[i][j] + macroDense[i][j]*s(k, macroVel[i][j]);
                }
            }
        }
    }

    public void movePartsOld () {
        boolean[][] nextPart = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if((zouHe[i][j] & 2) != 0){
                    isPart[i][j] = true;
                }
                if(isPart[i][j]) {
                    double max = 0;
                    int maxInd = 0;
                    for (int k = 0; k < 9; k++) {
                        if (Math.abs(lattice[i][j][k])/w[k] >= max) {
                            maxInd = k;
                            max = lattice[i][j][k]/w[k];
                        }
                    }

                    if(i + e[maxInd][0] >= 0 && i + e[maxInd][0] < width && j + e[maxInd][1] >= 0 && j + e[maxInd][1] < height){
                        nextPart[i + (int)e[maxInd][0]][j + (int)e[maxInd][1]] = true;
                    }

//                    System.out.println(maxInd + "     "  +max);
                }
            }
        }
        isPart = nextPart;
    }

    public void moveParts () {
        boolean[][] nextPart = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
//                if((zouHe[i][j] & 2) != 0){
//                    isPart[i][j] = true;
//                }
                if(isPart[i][j]) {
                    double thresh = 0.05;

                    int vert = 0;
                    int horz = 0;

                    if(macroVel[i][j][0] > thresh){
                        horz = 1;
                    }else if(macroVel[i][j][0] < -thresh){
                        horz = -1;
                    }

                    if(macroVel[i][j][1] > thresh){
                        vert = 1;
                    }else if(macroVel[i][j][1] < -thresh){
                        vert = -1;
                    }

                    if(i + horz >= 0 && i + horz < width && j + vert >= 0 && j + vert < height){
                        nextPart[i + horz][j + vert] = true;
                    }

//                    System.out.println(maxInd + "     "  +max);
                }
            }
        }
        isPart = nextPart;
    }

    public void step () {
        stream2();
        updateMDMVED();
        collide();
        clampAll();
        moveParts();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
//                g2d.setColor(Color.BLUE);

                int val = (int) clamp(0,255,Math.abs(macroDense[i][j]*denseMult)); // density
                int val1 = (int) clamp(0,255,Math.abs(macroVel[i][j][0]*velMult)); // horizontal speed
                double val1d = clamp(0,255,Math.abs(macroVel[i][j][0]*velMult)); // horizontal speed
                int val2 = (int) clamp(0,255,Math.abs(macroVel[i][j][1]*velMult)); // vertical speed
                double val2d = clamp(0,255,Math.abs(macroVel[i][j][1]*velMult)); // vertical speed
                int val3 = (int) clamp(0,255, val1+val2); // total speed
                int val3half = (int) clamp(0,255, (val1d*val1d + val2d*val2d)/sqrMult); // total speed
                int val4 = (int) clamp(0,255,macroVel[i][j][0]*velMult); // positive horizontal
                int val5 = (int) clamp(0,255,-macroVel[i][j][0]*velMult); // negative horizontal
                int val6 = (int) clamp(0,255,macroVel[i][j][1]*velMult); // positive vert
                int val7 = (int) clamp(0,255,-macroVel[i][j][1]*velMult); // negative vert
                int val8 = (int) clamp(0,255,isPart[i][j]?partTop:partBot); // has particle?

                if(isWall[i][j]){
                    g2d.setColor(new Color(0, 0, 0));
                }else {
                    if(curCol == color.denseVel) {
                        g2d.setColor(Color.getHSBColor(0.5f - (val3half / 512f), 1f, val / 255f)); // hue for speed, value for density
                    }else if(curCol == color.partVel) {
                        g2d.setColor(Color.getHSBColor(0.5f - (val3half / 512f), 1f, val8 / 255f)); // hue for speed, value for particle
                    }else {
//                    g2d.setColor(new Color(val3, val, 255-val3)); // speed + dense
//                    g2d.setColor(new Color(val, val, val)); // all density
//                    g2d.setColor(new Color(val6, val4/4, val7)); // speed
                    }
                }
                g2d.drawLine(i,j,i,j);

//                System.out.print(macroDense[i][j] + " ");
            }
//            System.out.println("");
        }
        g2d.dispose();
    }
}
