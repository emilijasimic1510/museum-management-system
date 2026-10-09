import { TestBed } from '@angular/core/testing';
import { QrCamera, QrDecoder } from './qr-camera';

describe('Visitor camera',()=>{
  const trackStop=vi.fn(),controlsStop=vi.fn();
  let found:(text:string)=>void;
  const start=vi.fn(async(_stream:MediaStream,_video:HTMLVideoElement,callback:(text:string)=>void)=>{found=callback;return {stop:controlsStop};});
  const media=vi.fn(async()=>({getTracks:()=>[{stop:trackStop}]} as unknown as MediaStream));
  beforeEach(()=>{vi.clearAllMocks();Object.defineProperty(window,'isSecureContext',{configurable:true,value:true});Object.defineProperty(navigator,'mediaDevices',{configurable:true,value:{getUserMedia:media}});TestBed.configureTestingModule({providers:[{provide:QrDecoder,useValue:{start}}]});});
  it('reads a QR without BarcodeDetector and releases the camera before emitting once',async()=>{
    const f=TestBed.createComponent(QrCamera);f.detectChanges();const result=vi.fn();f.componentInstance.decoded.subscribe(result);
    await f.componentInstance.start();expect(media).toHaveBeenCalledWith({video:{facingMode:{ideal:'environment'}},audio:false});
    found('COLLECTION-QR');found('COLLECTION-QR');expect(result).toHaveBeenCalledTimes(1);expect(result).toHaveBeenCalledWith('COLLECTION-QR');expect(trackStop).toHaveBeenCalled();expect(controlsStop).toHaveBeenCalled();
  });
  it('explains insecure LAN HTTP without requesting permission',async()=>{
    Object.defineProperty(window,'isSecureContext',{configurable:true,value:false});const f=TestBed.createComponent(QrCamera);f.detectChanges();await f.componentInstance.start();expect(media).not.toHaveBeenCalled();expect(f.componentInstance.error()).toBe('camera.https');
  });
  it('stops a permission request that resolves after the user closes the camera',async()=>{
    let resolve!:(stream:MediaStream)=>void;media.mockImplementationOnce(()=>new Promise(r=>resolve=r));const f=TestBed.createComponent(QrCamera);f.detectChanges();const pending=f.componentInstance.start();f.componentInstance.stop();resolve({getTracks:()=>[{stop:trackStop}]} as unknown as MediaStream);await pending;expect(start).not.toHaveBeenCalled();expect(trackStop).toHaveBeenCalled();
  });
  it('releases the camera when leaving the page',async()=>{const f=TestBed.createComponent(QrCamera);f.detectChanges();await f.componentInstance.start();f.destroy();expect(trackStop).toHaveBeenCalled();expect(controlsStop).toHaveBeenCalled();});
  it('distinguishes permission refusal',async()=>{media.mockRejectedValueOnce(new DOMException('Denied','NotAllowedError'));const f=TestBed.createComponent(QrCamera);f.detectChanges();await f.componentInstance.start();expect(f.componentInstance.error()).toBe('camera.denied');expect(f.componentInstance.opened()).toBe(false);});
});
