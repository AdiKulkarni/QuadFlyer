# QuadFlyer
Adaptive FPV system for piloting UAV's over 3G/4G.

##Depedencies
This project uses open source libraries https://github.com/fyhertz/libstreaming and https://github.com/mik3y/usb-serial-for-android to build upon.

##Usage
This project is mainly developed to provide low delay FPV piloting system for UAV flying. It has following functional aspects.

1] Acts as a communication link between UAV and GCS for control and telemetry data transfer over 3G/4G. For more information on this please visit https://github.com/AdiKulkarni/GCS-CLI-Pilot.

2] Replaces typical RC controlled flying mechanism with internet communication.

3] Provides two types of adaptive streaming of live video from UAV to GCS.
  
  1. x264 encoded video streaming over RTP. This streaming method is similar to that of https://github.com/fyhertz/libstreaming
  2. MJPEG based video streaming over UDP. For more information on this please visit https://github.com/AdiKulkarni/GCS-CLI-Pilot. 

