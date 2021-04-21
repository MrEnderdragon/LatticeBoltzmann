import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class graphicsDriver extends JFrame {

    private static javax.swing.JFrame frame;
    private static Lattice lat;
    private static boolean running;

    public static class update extends TimerTask {

        private long count = 0;

        @Override
        public void run() {
//            if(count == 500) {
//                for (int i = 0; i < lat.width; i++) {
//                    for (int j = 0; j < lat.height; j++) {
//                        if ((j - 110) * (j - 110) + (i - 75) * (i - 75) < 500) lat.isWall[i][j] = true;
//                    }
//                }
//            }


            //cylinder
//            for (int i = 0; i < lat.width; i++) {
//                for (int j = 0; j < lat.height; j++) {
//                    if ((j - 110) * (j - 110) + (i - 75) * (i - 75) < 500) lat.isWall[i][j] = true;
//                }
//            }

            if(running) {
                lat.step();
            }

            frame.revalidate();
            frame.repaint();

            count++;

//            System.out.println("update");
        }
    }

    public static void main(String[] args) {
//        int frameWidth = 500; // Poiseuille flow
//        int frameHeight = 200;

        int frameWidth = 256; // moving lid
        int frameHeight = 256;

        frame = new javax.swing.JFrame();
        frame.setSize(frameWidth+100, frameHeight+100);
        frame.setLayout(new BorderLayout());

        lat = new Lattice(frameWidth, frameHeight, Lattice.setup.MoveLid, Lattice.color.denseVel);

        // ~~~~~~~~~~~~~~~~    Poiseuille flow ~~~~~~~~~~~~~~~~

//        for (int i = 2; i < lat.height-2; i++) {
//            lat.zouHe[1][i] = 3;
//            lat.zouHeOr[1][i] = 1;
//            lat.zhVel[1][i][0] = 1d/2;
//            lat.zhPres[1][i] = 1;
//        }
//
//        for (int i = 2; i < lat.height-2; i++) {
//            lat.zouHe[lat.width-2][i] = 1;
//            lat.zouHeOr[lat.width-2][i] = 3;
////                lat.zhVel[lat.width-2][i][0] = -1d/7;
//            lat.zhPres[lat.width-2][i] = -0;
//        }

        // ~~~~~~~~~~~~~~~~ moving lid ~~~~~~~~~~~~~~~~

//        for (int i = 2; i < lat.height-2; i++) {
//            lat.zouHe[1][i] = 2;
//            lat.zouHeOr[1][i] = 1;
//            lat.zhVel[1][i][1] = 1d/20;
////            lat.zhPres[1][i] = 1;
//        }

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        controlPanel.setSize(10,10);

        JButton run = new JButton("run");
        JButton pause = new JButton("pause");
        JButton resetWalls = new JButton("reset walls");
        JButton resetFlow = new JButton("reset flow");
        JButton resetAll = new JButton("reset all");

        JPanel colPan = new JPanel();
        colPan.setLayout(new GridLayout(0,1));
        colPan.setPreferredSize(new Dimension(100, 10));

        JButton denseVel = new JButton("Dense + Vel");
        JButton partVel = new JButton("Part + Vel");

        JLabel info = new JLabel("Dense: 0    |     Vel: 0,0", JLabel.CENTER);
        info.setText("tmp");

        running = false;
        pause.setEnabled(false);

        run.addActionListener(e -> {
            running = true;
            run.setEnabled(false);
            pause.setEnabled(true);
        });
        pause.addActionListener(e -> {
            running = false;
            run.setEnabled(true);
            pause.setEnabled(false);
        });
        resetWalls.addActionListener(e -> {
            running = false;
            run.setEnabled(true);
            pause.setEnabled(false);

            lat.resetWalls();
            frame.revalidate();
            frame.repaint();
        });
        resetFlow.addActionListener(e -> {
            running = false;
            run.setEnabled(true);
            pause.setEnabled(false);

            lat.resetFlow();
            frame.revalidate();
            frame.repaint();
        });
        resetAll.addActionListener(e -> {
            running = false;
            run.setEnabled(true);
            pause.setEnabled(false);

            lat.resetWalls();
            lat.resetFlow();
            frame.revalidate();
            frame.repaint();
        });

        denseVel.addActionListener(e -> {
            lat.curCol = Lattice.color.denseVel;
            denseVel.setEnabled(false);
            partVel.setEnabled(true);
        });
        partVel.addActionListener(e -> {
            lat.curCol = Lattice.color.partVel;
            denseVel.setEnabled(true);
            partVel.setEnabled(false);
        });

        lat.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j < 1; j++) {
                        if(e.getX()+i >= 0 && e.getX()+i < lat.width && e.getY()+j >= 0 && e.getY()+j < lat.height) {
                            if(lat.curCol == Lattice.color.denseVel){
                                lat.isWall[e.getX() + i][e.getY() + j] = SwingUtilities.isLeftMouseButton(e);
                            }else if(lat.curCol == Lattice.color.partVel){
                                lat.isPart[e.getX() + i][e.getY() + j] = SwingUtilities.isLeftMouseButton(e);
                            }
                        }
                    }
                }

                frame.revalidate();
                frame.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if(e.getX() >= 0 && e.getX() < lat.width && e.getY() >= 0 && e.getY() < lat.height)
                    info.setText("Dense: " + lat.macroDense[e.getX()][e.getY()] + "    |     Vel: " +
                            lat.macroVel[e.getX()][e.getY()][0] + "," + lat.macroVel[e.getX()][e.getY()][1] + "    |     Part: " +
                            (lat.isPart[e.getX()][e.getY()]?"yes":"no"));
            }
        });


        controlPanel.add(run);
        controlPanel.add(pause);
        controlPanel.add(resetWalls);
        controlPanel.add(resetFlow);
        controlPanel.add(resetAll);

        colPan.add(denseVel);
        colPan.add(partVel);

        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(colPan, BorderLayout.EAST);
        frame.add(lat, BorderLayout.CENTER);
        frame.add(info, BorderLayout.SOUTH);

        frame.setVisible(true);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Timer timer = new Timer();
        timer.schedule(new update(), 0, 1);
    }
}
