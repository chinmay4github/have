# AI Engineer Interview Questions

A company-wise collection of **publicly reported interview questions** and
**original practice questions** for AI engineering preparation.

**Last researched: September 30, 2026.** This is a manually maintained guide,
not a live interview feed.

> No public question bank can cover every company or guarantee what your
> interviewer will ask. Interviews vary by team, role, seniority, location, and
> date. Candidate reports are not employer-confirmed interview scripts.

## Start here

- [Company-wise reported questions](docs/company-questions.md): 52 paraphrased
  questions across 12 companies, with source links and evidence labels.
- [Practice bank](docs/practice-questions.md): 40 original questions with answer
  checklists covering ML, LLMs, RAG, agents, serving, evaluation, coding, and
  behavioral interviews.
- [Source notes](docs/sources.md): reported roles, access limitations, date
  uncertainty, and the distinction between reports and compilations.
- [Contribute a question or another company](CONTRIBUTING.md).

## Company index

The roles below describe the cited reports, not all roles at each company.
Several are adjacent SWE or MLE roles rather than jobs titled AI Engineer.

| Company | Reported role or scope | Evidence | Questions | Sources |
| --- | --- | --- | ---: | --- |
| [Amazon](docs/company-questions.md#amazon) | Machine Learning Engineer | I | 6 | [1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm) |
| [Anthropic](docs/company-questions.md#anthropic) | SWE, Safeguards; ML and serving compilation | C + S | 7 | [8](https://www.tryexponent.com/experiences/anthropic-senior-software-engineer-interview-2ffa5f), [9](https://www.tryexponent.com/blog/ai-engineer-interview-questions) |
| [Databricks](docs/company-questions.md#databricks) | Role unspecified in compilation | S | 1 | [9](https://www.tryexponent.com/blog/ai-engineer-interview-questions) |
| [Glean](docs/company-questions.md#glean) | New-grad engineering | C | 4 | [7](https://leetcode.com/discuss/interview-question/6487496/Glean-or-New-Grad-Interview-Full-Loop-Experience/) |
| [Google](docs/company-questions.md#google) | AI Engineer Intern; Machine Learning Engineer | C + I | 7 | [1](https://www.tryexponent.com/experiences/google-machine-learning-engineer-intern-interview-d43fed), [2](https://www.glassdoor.com/Interview/Google-Machine-Learning-Engineer-Interview-Questions-EI_IE9079.0,6_KO7,32.htm) |
| [Meta](docs/company-questions.md#meta) | Machine Learning Engineer | I | 4 | [5](https://www.glassdoor.co.in/Interview/Meta-Machine-Learning-Engineer-Interview-Questions-EI_IE40772.0,4_KO5,30.htm) |
| [Microsoft](docs/company-questions.md#microsoft) | Machine Learning Engineer | I | 4 | [3](https://www.glassdoor.ca/Interview/Microsoft-Machine-Learning-Engineer-Interview-Questions-EI_IE1651.0,9_KO10,35.htm) |
| [NVIDIA](docs/company-questions.md#nvidia) | AI Engineer | I | 3 | [4](https://www.glassdoor.com/Interview/NVIDIA-AI-Engineer-Interview-Questions-EI_IE7633.0,6_KO7,18.htm) |
| [OpenAI](docs/company-questions.md#openai) | Full Stack Engineer, Applied | C | 5 | [5](https://www.tryexponent.com/experiences/openai-software-engineer-interview-5a7d13) |
| [Scale AI](docs/company-questions.md#scale-ai) | New Grad Software Engineer | C | 4 | [2](https://www.tryexponent.com/experiences/scale-ai-software-engineer-interview-b83485) |
| [Sierra AI](docs/company-questions.md#sierra-ai) | Agent Engineer | C | 4 | [1](https://www.tryexponent.com/experiences/sierra-ai-machine-learning-engineer-interview-8549fc) |
| [xAI](docs/company-questions.md#xai) | Member of Technical Staff | C | 3 | [1](https://www.tryexponent.com/experiences/x-ai-software-engineer-interview-7fbd12) |

### Evidence labels

| Label | Meaning | Limitation |
| --- | --- | --- |
| **C** | Public candidate report; relevant full text reviewed | Self-reported, not independently confirmed by the employer |
| **I** | Candidate report visible in a search-index excerpt only | Full-page access was blocked; dates and context may be incomplete |
| **S** | Secondary compilation attributes the question to a company | The underlying individual interview was not reviewed |
| **P** | Original practice question written for this guide | No claim that any company asked it |

A publisher's “verified” badge is not treated as independent verification here.
A publication or page-update date is not an interview date. See the
[source notes](docs/sources.md) before drawing conclusions about freshness.

## How to use this guide

1. **Confirm your actual loop.** Ask the recruiter about the role, rounds,
   permitted languages, and whether AI assistance is allowed. Do not assume
   another candidate's tool policy applies to you.
2. **Choose relevant reports.** An intern coding screen, an agent engineer
   take-home, and a research engineering loop require different preparation.
3. **Answer before reading the checklist.** Explain your assumptions and
   trade-offs aloud, then compare with the preparation pointers.
4. **Practice implementation.** Write runnable code, test edge cases, discuss
   complexity, and explain unfamiliar code rather than memorizing solutions.
5. **Prepare real project stories.** Use your own work and measured outcomes;
   do not invent experience to fit a question.

### Technical answer framework

The following is preparation advice, not a company's scoring rubric:

- Clarify users, inputs, outputs, permissions, and what success means.
- State quality, latency, availability, and cost requirements explicitly.
- Establish a simple baseline before adding models or agents.
- Explain data flow, component boundaries, and failure handling.
- Evaluate retrieval, generation, and tool execution separately when applicable.
- Include observability, security, staged rollout, and rollback.
- Compare alternatives and identify the next experiment you would run.

For behavioral questions, use **Situation → Task → Action → Result**, then
explain what you learned and what you would change.

## Suggested seven-day preparation plan

Adapt this schedule to your experience and interview date; it is not an offer
or readiness guarantee.

| Day | Focus | Deliverable |
| --- | --- | --- |
| 1 | ML, statistics, and data leakage | Explain a validation strategy and justify its metrics |
| 2 | LLM fundamentals and fine-tuning | Work through attention, token budgets, and model adaptation |
| 3 | RAG and retrieval | Sketch a permission-aware retrieval pipeline and its evaluation |
| 4 | Agents and safety | Build a small tool workflow with limits, tests, and a human fallback |
| 5 | Serving and production evaluation | Estimate memory; explain latency, cost, monitoring, and rollback |
| 6 | Coding and SQL | Complete two timed exercises and test their edge cases |
| 7 | Mock interview and project discussion | Run one design mock and rehearse two truthful project stories |

## Keeping the collection useful

Missing companies are welcome, but **sources come before company attribution**.
Unsourced questions belong in the practice bank, not in the reported list.
Follow the [contribution checklist](CONTRIBUTING.md#review-checklist) to add a
report, correct a citation, or flag an outdated entry.

This guide is for preparation before an interview. It is not a tool for covert
assistance during a live assessment. Respect interview rules, confidentiality,
and the original publishers' rights.
