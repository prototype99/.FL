package com.mmu;

import static org.libsdl.api.Sdl.SDL_Init;
import static org.libsdl.api.Sdl.SDL_Quit;
import static org.libsdl.api.error.SdlError.SDL_GetError;

public class Main {

    public static void main(String[] args) {
        if((SDL_Init() == -1)) {
            System.out.println(SDL_GetError());
        }
        SDL_Quit();
    }
}
