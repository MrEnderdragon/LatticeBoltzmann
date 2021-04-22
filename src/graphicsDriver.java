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
//    private static boolean running;
    private static drawType drT;
    private static Timer updTimer;
    private static Timer partTimer;


    public static class update extends TimerTask {

        @Override
        public void run() {
            lat.step();
            frame.revalidate();
            frame.repaint();
        }
    }

    public static class uptParts extends TimerTask {

        @Override
        public void run() {
            lat.moveParts();
            frame.revalidate();
            frame.repaint();
        }
    }

    static enum drawType {
        walls,
        particles
    }

    public static void main(String[] args) {
        int frameWidth = 500; // Poiseuille flow
        int frameHeight = 200;

//        int frameWidth = 256; // moving lid
//        int frameHeight = 256;

        frame = new javax.swing.JFrame();
        frame.setSize(frameWidth+500, frameHeight+200);
        frame.setLayout(new BorderLayout());

        lat = new Lattice(frameWidth, frameHeight, Lattice.setup.Poiseuille, Lattice.color.denseVel);

        updTimer = new Timer();
        partTimer = new Timer();

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
        JButton runParts = new JButton("run particles");
        JButton pauseParts = new JButton("pause particles");
        JButton resetWalls = new JButton("reset walls");
        JButton resetFlow = new JButton("reset flow");
        JButton resetAll = new JButton("reset all");

        JPanel colPan = new JPanel();
        colPan.setLayout(new GridLayout(0,1));
        colPan.setPreferredSize(new Dimension(100, 10));

        JButton denseVel = new JButton("Dense + Vel");
        JButton partVel = new JButton("Part + Vel");

        JPanel drawPan = new JPanel();
        drawPan.setLayout(new GridLayout(0,1));
        drawPan.setPreferredSize(new Dimension(100, 10));

        JButton drWalls = new JButton("Draw Walls");
        JButton drParts = new JButton("Draw Particles");

        JLabel info = new JLabel("Dense: 0    |     Vel: 0,0", JLabel.CENTER);
        info.setText("tmp");

//        running = false;
        drT = drawType.walls;

        pause.setEnabled(false);
        denseVel.setEnabled(false);
        drWalls.setEnabled(false);

        run.addActionListener(e -> {
            updTimer = new Timer();
            updTimer.schedule(new update(), 0, 1);
            partTimer.cancel();
            run.setEnabled(false);
            pause.setEnabled(true);
            runParts.setEnabled(false);
            pauseParts.setEnabled(false);
        });
        pause.addActionListener(e -> {
            updTimer.cancel();
            run.setEnabled(true);
            pause.setEnabled(false);
            runParts.setEnabled(true);
            pauseParts.setEnabled(false);
        });
        runParts.addActionListener(e -> {
            partTimer = new Timer();
            partTimer.schedule(new uptParts(), 0, 1);
            runParts.setEnabled(false);
            pauseParts.setEnabled(true);
        });
        pauseParts.addActionListener(e -> {
            partTimer.cancel();
            runParts.setEnabled(true);
            pauseParts.setEnabled(false);
        });
        resetWalls.addActionListener(e -> {
            updTimer.cancel();
            run.setEnabled(true);
            pause.setEnabled(false);

            lat.resetWalls();
            frame.revalidate();
            frame.repaint();
        });
        resetFlow.addActionListener(e -> {
            updTimer.cancel();
            run.setEnabled(true);
            pause.setEnabled(false);

            lat.resetFlow();
            frame.revalidate();
            frame.repaint();
        });
        resetAll.addActionListener(e -> {
            updTimer.cancel();
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
            frame.revalidate();
            frame.repaint();
        });
        partVel.addActionListener(e -> {
            lat.curCol = Lattice.color.partVel;
            denseVel.setEnabled(true);
            partVel.setEnabled(false);
            frame.revalidate();
            frame.repaint();
        });

        drWalls.addActionListener(e -> {
            drT = drawType.walls;
            drWalls.setEnabled(false);
            drParts.setEnabled(true);
        });
        drParts.addActionListener(e -> {
            drT = drawType.particles;
            drWalls.setEnabled(true);
            drParts.setEnabled(false);
        });

        lat.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j < 1; j++) {
                        if(e.getX()+i >= 0 && e.getX()+i < lat.width && e.getY()+j >= 0 && e.getY()+j < lat.height) {
                            if(drT == drawType.walls){
                                lat.isWall[e.getX() + i][e.getY() + j] = SwingUtilities.isLeftMouseButton(e);
                            }else if(drT == drawType.particles){
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
        controlPanel.add(runParts);
        controlPanel.add(pauseParts);
        controlPanel.add(resetWalls);
        controlPanel.add(resetFlow);
        controlPanel.add(resetAll);

        colPan.add(denseVel);
        colPan.add(partVel);

        drawPan.add(drWalls);
        drawPan.add(drParts);

        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(colPan, BorderLayout.EAST);
        frame.add(drawPan, BorderLayout.WEST);

        frame.add(lat, BorderLayout.CENTER);
        frame.add(info, BorderLayout.SOUTH);

        frame.setVisible(true);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
