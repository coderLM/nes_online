
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
var framebuffer_int = new Int32Array(FRAMEBUFFER_SIZE);
var nes = new NES({
	onFrame: function (framebuffer_24) {
		for (var i = 0; i < FRAMEBUFFER_SIZE; i++) {
			// framebuffer_u32[i] = 0xFF000000 | framebuffer_24[i];
			framebuffer_int[i] = framebuffer_24[i];
		}
		push();
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

function nes_start(rom_data) {

	var array = new Uint8Array(rom_data);
	printData(array);
	var string = "";
	for (var i = 0; i < array.length; i++) {
		string += String.fromCharCode(array[i]);
	}
	nes_init();
	nes.loadROM(string);
}

function nes_init() {
	var buffer = new ArrayBuffer(SCREEN_WIDTH * SCREEN_HEIGHT);
	framebuffer_u8 = new Uint8ClampedArray(buffer);
	framebuffer_u32 = new Uint32Array(buffer);
}
function frame() {
	nes.frame();
}
function push() {
	///push frame
	java_receive_frame(framebuffer_int);

	///push audio
	var len = audio_remain();
	if (len < 1) return;
	var pushAudio = new Float32Array(len * 2);
	var index = 0;
	for (var i = 0; i < len; i++) {
		var src_idx = (audio_read_cursor + i) & SAMPLE_MASK;
		pushAudio[index] = audio_samples_L[src_idx];
		pushAudio[index + 1] = audio_samples_R[src_idx];
		index += 2;
	}
	audio_read_cursor = (audio_read_cursor + len) & SAMPLE_MASK;
	java_receive_audio(pushAudio);
}

function onKeyDown(keyCode) {
	keyboard(nes.buttonDown, keyCode);
}
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

function printData(data) {
	var printData = "";
	for (let i = 500; i < 600; i++) {
		printData += "," + data[i];
	}
	for (let i = 3500; i < 3600; i++) {
		printData += "," + data[i];
	}
	print("printData:::" + printData);
}
