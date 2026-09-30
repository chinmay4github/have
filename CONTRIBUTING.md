# Contributing

Help expand this guide company by company without turning preparation advice
into unsupported claims about real interviews.

[Home](README.md) · [Reported questions](docs/company-questions.md) ·
[Practice bank](docs/practice-questions.md) · [Source notes](docs/sources.md)

## Add a reported question

1. Find a public candidate report or a clearly attributed compilation.
2. Preserve the company, reported role, level, and location when available.
   Do not rename an SWE or MLE report as an AI Engineer interview.
3. Read the relevant source text. If you only have a search-index excerpt,
   explicitly label that access limitation.
4. Write a short paraphrase. Keep missing specifications missing, and put any
   suggested assumptions or follow-ups in preparation pointers.
5. Add a source link and evidence label to each question row in
   `docs/company-questions.md`.
6. Add or update the matching company section in `docs/sources.md`.
7. Update the company index and question totals in `README.md`, plus the
   evidence totals in `docs/sources.md`.

Use the labels consistently:

| Label | Use when |
| --- | --- |
| C | The relevant full public candidate report was reviewed |
| I | Only candidate-report search-index text was available |
| S | A secondary compilation attributes the item to a company |
| P | It is an original practice question with no company attribution |

Neither C nor a publisher's “verified” badge means employer confirmation.
A source listing several questions is not proof of several independent
interviews. Multiple websites republishing one article are not corroboration.

### Required source metadata

- Publisher and public HTTPS URL.
- Company and role as described in the source.
- Seniority and location, or `Not stated`.
- Interview date, or `Unknown / relative date only`.
- Report publication or update date, if explicitly available; keep this
  separate from the interview date.
- Review date and access outcome.
- Evidence label, question IDs supported, and known limitations.

Do not infer an interview year from an article's title or assign one review's
date to other reviews on a shared listing page.

### Entry template

Use the next unused company-prefixed ID; never renumber existing entries just
to insert a new question. Add a company heading and source metadata before its
table when introducing a company.

```markdown
| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| ABC-01 | Brief paraphrase of the public report. | Our own preparation advice. | C · [number](https://example.com/public-report) |
```

Replace all placeholders. Citation numbers are source labels, not question IDs
or confidence scores. Always include the URL; do not assume a number alone
uniquely identifies a source.

## Add a practice question

No suitable evidence of a company attribution? Add an independently written
exercise to `docs/practice-questions.md` with the next unused `P-` ID, a depth
label, and a concise answer checklist. Do not add an “asked at” company tag.

Practice questions should encourage reasoning, implementation, tests, and
trade-offs rather than memorizing product names. Full sample answers, if added
later, must be original and explicit about assumptions.

## Correct or retire an attribution

Describe what changed: broken link, restricted access, misidentified role,
missing context, wrong date, or unsupported company attribution. Recheck the
source and update its note. Downgrade the evidence label when needed; remove or
flag an unsupported reported claim rather than disguising it as verified.

Do not claim that checking a URL establishes the question is still used.
Only update the guide's research date after an actual content review, and
retain per-source dates for entries reviewed at different times.

## Review checklist

- [ ] Every reported question has a public source and the correct evidence label.
- [ ] Roles and known limitations match the source; unknown fields remain unknown.
- [ ] Interview, publication, update, and research dates are not conflated.
- [ ] Prompts are paraphrased; hints are clearly editorial advice.
- [ ] No unsupported “all companies,” frequency, or guaranteed-outcome claims.
- [ ] New IDs are unique; README and evidence counts are updated.
- [ ] Relative file links and heading anchors open correctly.
- [ ] `git diff --check` passes.
- [ ] No confidential assessments, private interview material, personal data,
  credentials, or copied paid answer banks are included.

This repository is for preparation before an interview, not concealed help
during one. Respect assessment rules, confidentiality agreements, access
restrictions, and publishers' rights.
