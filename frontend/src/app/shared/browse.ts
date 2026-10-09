import { Exhibition, Term } from '../core/models';

export function browsableExhibition(e:Exhibition,now=Date.now()) {
  return e.onlineVisible && ['AKTIVNA','PLANIRANA'].includes(e.status) && Date.parse(e.end)>now;
}

export function browsableTerm(t:Term,now=Date.now()) {
  // An ongoing slot remains available under the existing sales rules.
  return ['OTVOREN','POPUNJEN'].includes(t.status) && Date.parse(t.end)>now;
}
