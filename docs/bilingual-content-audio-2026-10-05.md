# Bilingual content and audio (2026-10-05)

The existing `DigitalContent` model already carries `language` and `version`, so the database schema and migrations were left unchanged. Curators can save separate `TEKST` versions in `sr` and `en` for one exhibit participation. Text is stored as UTF-8 files below `MUSEUM_TEXT_STORAGE_PATH` (default `./runtime/text`); only an opaque reference is kept in the database.

Visitor content remains protected by the existing active-visit and scan checks. The visitor UI selects the newest text and audio in the selected UI language. English falls back to Serbian when no English version exists; Serbian never selects English. Private text and audio references are hidden from metadata responses and are served only through the guarded visit endpoints.

Audio continues to use eSpeak NG. The curator can request `sr` or `en`; each generated WAV is a new language-specific version in the existing audio storage. English generation requires an English text version (there is no automatic translation). Serbian retains the existing contextual-description fallback for older records without a text version.

Changed areas: `backend/.../text` (storage/service/controller), bilingual audio selection, protected content metadata, curator content editor, visitor text display, and the text storage property in `application.yml`.

Validation: frontend TypeScript noEmit passes. Maven compilation/tests could not run in this environment because dependency resolution is blocked by the sandbox network permission; the required escalation was rejected by the automatic approval service. No commit or push was performed.
