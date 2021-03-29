import javax.swing.*;
import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class graphicsDriver extends JFrame {

    private static javax.swing.JFrame frame;
    private static Lattice lat;

    public static class update extends TimerTask {

        private long count = 0;

        @Override
        public void run() {
            for (int i = 2; i < lat.height-2; i++) {
//                lat.lattice[i][1][2] += 1d/18;
//                lat.lattice[i][1][5] += 1d/27;
//                lat.lattice[i][1][6] += 1d/27;
                lat.zouHe[1][i] = 3;
                lat.zouHeOr[1][i] = 1;
                lat.zhVel[1][i][0] = 1d/1;
                lat.zhPres[1][i] = 1;
            }

            for (int i = 2; i < lat.height-2; i++) {
//                lat.lattice[i][1][2] += 1d/18;
//                lat.lattice[i][1][5] += 1d/27;
//                lat.lattice[i][1][6] += 1d/27;
                lat.zouHe[lat.width-2][i] = 1;
                lat.zouHeOr[lat.width-2][i] = 3;
//                lat.zhVel[lat.width-2][i][0] = -1d/7;
                lat.zhPres[lat.width-2][i] = -0.3;
            }

            if(count == 500) {
                for (int i = 0; i < lat.width; i++) {
                    for (int j = 0; j < lat.height; j++) {
                        if ((j - 110) * (j - 110) + (i - 75) * (i - 75) < 500) lat.isWall[i][j] = true;
                    }
                }
            }

            lat.step();

            frame.revalidate();
            frame.repaint();

            count++;

//            System.out.println("update");
        }
    }

    public static void main(String[] args) {
        int frameWidth = 500;
        int frameHeight = 200;
        frame = new javax.swing.JFrame();
        frame.setSize(frameWidth+100, frameHeight+100);
        lat = new Lattice(frameWidth, frameHeight);
//        for (int i = 30; i < lat.width-30; i++) {
//            lat.lattice[i][i][5] += 1;
//                lat.lattice[i][i][6] += 1d/27;
//                lat.lattice[i][i][8] += 1d/27;
//        }
        frame.add(lat);
        frame.setVisible(true);

        Timer timer = new Timer();
        timer.schedule(new update(), 0, 1);
    }
}
