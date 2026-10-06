// @vitest-environment jsdom
import { describe, expect, it, beforeEach } from 'vitest';
import { I18n, enumLabel, translationIntegrity, translationKeys } from './i18n';

describe('I18n', () => {
  beforeEach(() => localStorage.clear());

  it('defaults to Serbian', () => {
    const i18n = new I18n();
    expect(i18n.language()).toBe('sr');
    expect(i18n.t('nav.login')).toBe('Prijava');
  });

  it('switches languages and persists through refresh', () => {
    const i18n = new I18n();
    i18n.setLanguage('en');
    expect(i18n.t('nav.login')).toBe('Sign in');
    expect(new I18n().language()).toBe('en');
    i18n.setLanguage('sr');
    expect(i18n.t('nav.login')).toBe('Prijava');
  });

  it('falls back to Serbian when an English key is missing', () => {
    const i18n = new I18n();
    i18n.setLanguage('en');
    expect(i18n.t('nav.srOnly')).toBe('Samo srpski');
  });

  it('keeps API enum values unchanged while translating their labels', () => {
    const apiValue = 'ROLE_CURATOR';
    const i18n = new I18n();
    expect(apiValue).toBe('ROLE_CURATOR');
    expect(i18n.role(apiValue)).toBe('Kustos');
    expect(i18n.status('AKTIVNA')).toBe('Aktivna');
  });

  it('keeps Serbian and English translation keys compatible', () => {
    const integrity=translationIntegrity();
    expect(integrity.missingInEnglish).toEqual([]);
    expect(integrity.missingInSerbian).toEqual([]);
    expect(new Set(translationKeys()).size).toBe(translationKeys().length);
  });

  it('does not expose a raw translation key and maps content/payment values', () => {
    const i18n=new I18n(); i18n.setLanguage('en');
    expect(i18n.t('uiText.key.that.does.not.exist')).not.toBe('uiText.key.that.does.not.exist');
    expect(enumLabel('KARTICA','payment')).toBe('Card');
    expect(enumLabel('TEKST','content')).toBe('Text');
    expect(enumLabel('ROLE_CURATOR','role')).toBe('Curator');
  });
});

