package com.nulllab.ble.coding.central;

import java.util.LinkedHashMap;
import java.util.Map;

public class DemoCode {
    public final static Map<String, String> Codes = new LinkedHashMap<>();

    static {
        Codes.put("print", "example_code/print/main.py");
        Codes.put("read digital", "example_code/read_digital/main.py");
        Codes.put("write digital", "example_code/write_digital/main.py");
        Codes.put("read adc", "example_code/read_adc/main.py");
        Codes.put("read digital and adc", "example_code/read_digital_and_adc/main.py");
        Codes.put("stdio", "example_code/stdio/main.py");
        Codes.put("joystick", "example_code/joystick/main.py");
        Codes.put("dht11", "example_code/dht11/main.py");
        Codes.put("passive buzzer", "example_code/passive_buzzer/main.py");
        Codes.put("ir remote control receiver", "example_code/ir_remote_control_receiver/main.py");
        Codes.put("dc motor", "example_code/dc_motor/main.py");
        Codes.put("servo", "example_code/servo/main.py");
        Codes.put("geek servo 270", "example_code/geek_servo_270/main.py");
        Codes.put("servo 360", "example_code/servo360/main.py");
        Codes.put("ws2812", "example_code/ws2812/main.py");
        Codes.put("ds18b20", "example_code/ds18b20/main.py");
        Codes.put("ultrasonic one wire", "example_code/ultrasonic_one_wire/main.py");
        Codes.put("rgb led", "example_code/rgb_led/main.py");
        Codes.put("tm1650 four digit led", "example_code/tm1650_four_digit_led/main.py");
        Codes.put("x16k33 matrix led 5x5", "example_code/x16k33_matrix_led_5x5/main.py");
        Codes.put("ssd1306 i2c 128x64", "example_code/ssd1306_i2c_128x64/main.py");
        Codes.put("qma6100p measure gesture", "example_code/qma6100p/measure_gesture/main.py");
        Codes.put("qma6100p read acceleration", "example_code/qma6100p/read_acceleration/main.py");
        Codes.put("traffic lights", "example_code/traffic_lights/main.py");
        Codes.put("adc five button", "example_code/adc_five_button/main.py");
    }
}
