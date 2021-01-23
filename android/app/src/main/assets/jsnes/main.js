
var NES = require("./src/nes");

var SCREEN_WIDTH = 256;
var SCREEN_HEIGHT = 240;
var FRAMEBUFFER_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT;

var canvas_ctx, image;
var framebuffer_u8, framebuffer_u32;

var AUDIO_BUFFERING = 512;
var SAMPLE_COUNT = 4 * 1024;
var SAMPLE_MASK = SAMPLE_COUNT - 1;
var audio_samples_L = new Float32Array(SAMPLE_COUNT);
var audio_samples_R = new Float32Array(SAMPLE_COUNT);
var audio_write_cursor = 0, audio_read_cursor = 0;
var printCount = 0;
var nes = new NES({
	onFrame: function (framebuffer_24) {
		for (var i = 0; i < FRAMEBUFFER_SIZE; i++) {
			framebuffer_u32[i] = 0xFF000000 | framebuffer_24[i];
		}	
		if (printCount % 50 == 0) {
			print("onFrame:" + JSON.stringify(framebuffer_24));
		}
		printCount++;
		print("onFrame:" + printCount);
	},
	onAudioSample: function (l, r) {
		audio_samples_L[audio_write_cursor] = l;
		audio_samples_R[audio_write_cursor] = r;
		audio_write_cursor = (audio_write_cursor + 1) & SAMPLE_MASK;
	},
});

function audio_remain() {
	return (audio_write_cursor - audio_read_cursor) & SAMPLE_MASK;
}

function nes_start(data) {
	nes_init();

	print("data js len:" + data.length);
	print("data js :" + data.substring(0, 20) + "...");
	nes.loadROM(data);
}

function nes_init() {
	var buffer = new ArrayBuffer(SCREEN_WIDTH * SCREEN_HEIGHT);
	framebuffer_u8 = new Uint8ClampedArray(buffer);
	framebuffer_u32 = new Uint32Array(buffer);
}
//called by java
function get_frame() {
	return framebuffer_u8;
}
//called by java
function get_audio(dataLen) {
	// Attempt to avoid buffer underruns.
	if (audio_remain() < AUDIO_BUFFERING) {
		nes.frame();
	}

	var dst = new Float32Array(dataLen * 2);
	for (var i = 0; i < dataLen; i++) {
		var src_idx = (audio_read_cursor + i) & SAMPLE_MASK;
		dst[i] = audio_samples_L[src_idx];
		dst[dataLen + i] = audio_samples_R[src_idx];
	}

	audio_read_cursor = (audio_read_cursor + dataLen) & SAMPLE_MASK;
	if (printCount % 2 == 0) {
		print("get_audio = "+ dst[0]+" "+dst[1]+" "+dst[2]+" "+dst[3]);
	}
	return dst;
}
//called by java
function onKeyDown(keyCode) {
	keyboard(nes.buttonDown, keyCode);
}
//called by java
function onKeyUp(keyCode) {
	keyboard(nes.buttonUp, keyCode);
}

function keyboard(callback, keyCode) {
	var player = 1;
	switch (keyCode) {
		case 38: // UP
			callback(player, jsnes.Controller.BUTTON_UP); break;
		case 40: // Down
			callback(player, jsnes.Controller.BUTTON_DOWN); break;
		case 37: // Left
			callback(player, jsnes.Controller.BUTTON_LEFT); break;
		case 39: // Right
			callback(player, jsnes.Controller.BUTTON_RIGHT); break;
		case 65: // 'a' - qwerty, dvorak
		case 81: // 'q' - azerty
			callback(player, jsnes.Controller.BUTTON_A); break;
		case 83: // 's' - qwerty, azerty
		case 79: // 'o' - dvorak
			callback(player, jsnes.Controller.BUTTON_B); break;
		case 9: // Tab
			callback(player, jsnes.Controller.BUTTON_SELECT); break;
		case 13: // Return
			callback(player, jsnes.Controller.BUTTON_START); break;
		default: break;
	}
}