package com.mmu;

import org.libsdl.api.event.events.SDL_ControllerSensorEvent;
import org.libsdl.api.gamecontroller.SDL_GameController;

public class GhostLog {
    boolean gyroStatus = true;
    int numSticks = 1;
    SDL_ControllerSensorEvent gyroEvent = null;
    SDL_GameController gamepad = null;
    String gamepadError = "";
}
