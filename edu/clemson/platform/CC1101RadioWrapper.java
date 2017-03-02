package edu.clemson.platform;

import se.sics.mspsim.chip.PacketListener;
import se.sics.mspsim.chip.Radio802154;
import se.sics.mspsim.platform.sky.RadioWrapper;
import se.sics.mspsim.util.Utils;

public class CC1101RadioWrapper extends RadioWrapper {

	public CC1101RadioWrapper(Radio802154 radio) {
		super(radio);
	}
	
	@Override
	public void receivedByte(byte data) {
		PacketListener listener = this.packetListener;
		//System.out.println("*** RF Data :" + data + " = $" + Utils.hex8(data) + " => " + data);
		if (pos == 0) {
			if (listener != null) {
				listener.transmissionStarted();
			}
			len = data;
		}
		buffer[pos++] = data;
		// len + 1 = pos + 5 (preambles)
		if (len > 0 && len + 1 == pos) {
			//System.out.println("***** SENDING DATA from CC2420 len = " + len);
			byte[] packet = new byte[len + 1];
			System.arraycopy(buffer, 0, packet, 0, len + 1);
			if (listener != null) {
				listener.transmissionEnded(packet);
			}
			pos = 0;
			len = 0;
		}
	}
}
