
CC=gcc
CFLAGS=-Wall -g
ANSI_C=-ansi -pedantic

all: vumeter-sdl

clean:
	rm -f vumeter-sdl

vumeter-sdl: vumeter.c
	$(CC) $(CFLAGS) vumeter.c -o vumeter-sdl -lm `sdl2-config --cflags --libs`
