package com.mmu;

import org.libsdl.api.event.events.SDL_ControllerSensorEvent;
import org.libsdl.api.event.events.SDL_Event;
import org.libsdl.api.gamecontroller.SDL_GameController;
import processing.core.PApplet;

import java.util.Arrays;

import static org.libsdl.api.SDL_SubSystem.SDL_INIT_GAMECONTROLLER;
import static org.libsdl.api.Sdl.SDL_Init;
import static org.libsdl.api.error.SdlError.SDL_GetError;
import static org.libsdl.api.event.SdlEvents.SDL_CONTROLLERSENSORUPDATE;
import static org.libsdl.api.event.SdlEvents.SDL_PollEvent;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerClose;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerHasSensor;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerOpen;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerSetSensorEnabled;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_IsGameController;
import static org.libsdl.api.joystick.SdlJoystick.SDL_NumJoysticks;
import static org.libsdl.api.sensor.SDL_SensorType.SDL_SENSOR_GYRO;
public class DotFL extends PApplet {
    boolean on = true;
    float circleSize,circleX,circleY;
    float[][] p = new float[0][2];
    //i feel like this might not be the cleverest way to error report, maybe i'm wrong though
    int error = 0;
    int health = 20;
    int numSticksNew;
    int numSticksOld = 1;
    //strings are predeclared to allow some cool math later. ye, i could probably use an enum but i've never liked them. also, less rewriting memory
    String[] msgsChange = new String[3];
    public static void main(String[] args) {
        PApplet.main("com.mmu.DotFL");
    }
    @Override
    public void setup() {
        clear();
        windowResizable(true);
        surface.setSize(displayWidth, displayHeight);
        cursor(CROSS);
        stroke(255);
        fill(255);
        //repetition free string construction~
        msgsChange[0] = "no ";
        for(int i = 0; i < 3; i++) {
            msgsChange[i] += "gamepad";
        }
        msgsChange[2] += "s";
        for(int i = 0; i < 3; i++) {
            msgsChange[i] += " connected";
        }
        //initialise sdl subsystems
        if((SDL_Init(SDL_INIT_GAMECONTROLLER) == -1)) {
            System.out.println(SDL_GetError());
        }
    }
    @Override
    public void draw() {
        clear();
        surface.setSize(width, height);
        numSticksNew = SDL_NumJoysticks();
        //output a message if required
        if(numSticksNew != numSticksOld) {
            int joyStatus = numSticksNew;
            if(joyStatus > 2) {
                joyStatus = 2;
            }
            System.out.println(msgsChange[joyStatus]);
        }
        //Check for joysticks
        if(numSticksNew > 0) {
            SDL_GameController DS5 = null;
            for(int i = 0; i < numSticksNew; i++) {
                if(SDL_IsGameController(i)) {
                    //Load DS5
                    DS5 = SDL_GameControllerOpen(i);
                }
            }
            if(DS5 == null) {
                System.out.println("Warning: Unable to open gamepad! SDL Error:" + SDL_GetError());
            } else {
                if(SDL_GameControllerHasSensor(DS5, SDL_SENSOR_GYRO)) {
                    if(SDL_GameControllerSetSensorEnabled(DS5, SDL_SENSOR_GYRO, true) == -1) {
                        System.out.println("Warning: unable to enable gyroscope");
                    } else {
                        SDL_Event e = new SDL_Event();
                        SDL_ControllerSensorEvent es;
                        SDL_PollEvent(e);
                        if(e.type == SDL_CONTROLLERSENSORUPDATE) {
                            es = e.csensor;
                            if(es.sensor == SDL_SENSOR_GYRO) {
                                System.out.println(Arrays.toString(es.data));
                            }
                        }
                    }
                } else {
                    System.out.println("Warning: no gyroscope detected, did you connect the right controller?");
                }
                SDL_GameControllerClose(DS5);
            }
        }
        strokeWeight(health/2);
        for(int i = 0;i < p.length;i ++) {
            float[] prev = p[i];
            if(i-1 >= 0) {
                prev = p[i-1];
            } else {
                if(!on) {
                    prev = p[p.length-1];
                }
            }
            //x is 0, y is 1, pretty simple really
            line(p[i][0], p[i][1], prev[0], prev[1]);
        }
        circle(circleX, circleY, circleSize);
        if(!on) {
            textSize(50);
            strokeWeight(1);
            text("FailPass".substring(error>20?0:4, 4 + (error>20?0:4)) + ": " + health + " health", 100, 100);
        }
        if(mousePressed) {
            if(on) {
                if(p.length > 0) {
                    var ps = p[p.length-1];
                    var angle = atan2(ps[1]-mouseY, ps[0]-mouseX);
                    var dis = dist(mouseX, mouseY, ps[0], ps[1]);
                    while(dis > 5) {
                        float[] appendVals = {ps[0]-5*cos(angle), ps[1]-5*sin(angle)};
                        append(p,appendVals);
                        ps = p[p.length-1];
                        angle = atan2(ps[1]-mouseY, ps[0]-mouseX);
                        dis = dist(mouseX, mouseY, ps[0], ps[1]);
                    }
                } else {
                    float[] appendVals = {mouseX, mouseY};
                    append(p,appendVals);
                }
            }
        }
        numSticksOld = numSticksNew;
    }
    @Override
    public void mouseReleased() {
        if(p.length > 10) {
            on = false;
            int x = 0;
            int y = 0;
            for (float[] floats : this.p) {
                x += floats[0];
                y += floats[1];
            }
            x /= this.p.length;
            y /= this.p.length;
            circleX = x;
            circleY = y;
            var dis = 0;
            for (float[] floats : this.p) {
                dis += dist(floats[0], floats[1], x, y);
            }
            dis /= this.p.length;
            circleSize = dis;
            error = 0;
            for (float[] floats : this.p) {
                this.error += abs(dist(floats[0], floats[1], x, y) - circleSize);
            }
            this.error /= this.p.length;
            if(circleSize < 25) {
                this.on = true;
            } else {
                health = ceil(20-this.error);
            }
        }
    }
}