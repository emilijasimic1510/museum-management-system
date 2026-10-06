import { scanToken, scanUrl } from './scan';
describe('QR input',()=>{
  it('accepts raw tokens and same-site installation URLs',()=>{
    expect(scanToken(' DEMO-QR-001 ')).toBe('DEMO-QR-001');
    expect(scanToken('/scan?token=opaque%2Btoken')).toBe('opaque+token');
    expect(scanToken('/scan/opaque%2Btoken')).toBe('opaque+token');
  });
  it('rejects foreign links and URLs without a token',()=>{
    expect(()=>scanToken('https://example.invalid/scan?token=abc')).toThrow();
    expect(()=>scanToken('/scan')).toThrow();
    expect(()=>scanToken('')).toThrow();
  });
  it('generates a same-origin path URL without embedding user or visit data',()=>{
    expect(scanUrl('opaque-token')).toBe(`${window.location.origin}/scan/opaque-token`);
  });
});
