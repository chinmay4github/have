# AI Engineer practice bank

[Home](../README.md) · [Reported questions](company-questions.md) ·
[Source notes](sources.md)

Every entry on this page has evidence label **P: original practice**. These
questions and answer checklists were written for this guide. They are **not
claims about questions asked by any particular company**, and the checklists
are not official interview scoring rubrics.

**Depth:** Foundation = explain a concept; Applied = implement or diagnose;
Advanced = reason about interacting production constraints. These labels are
preparation guidance, not job-level requirements.

Try each question aloud or in code before looking at its checklist. State
missing assumptions; there is rarely one universally correct architecture.

## ML and statistics

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-01 | Applied | Your offline classifier scores well, but production quality is poor. How would you audit its dataset and validation strategy? | Check temporal and entity leakage, point-in-time feature joins, duplicated examples, distribution shift, and a deployment-like holdout. |
| P-02 | Applied | A fraud model sees very few positive examples. How do you choose a metric and decision threshold when false positives and false negatives have different costs? | Define the decision cost; inspect precision-recall and calibration; tune on validation data; report subgroup results and operational alert volume. |
| P-03 | Foundation | Derive the gradient of binary logistic loss. What changes when the training data is perfectly separable? | Define logits and labels; derive the prediction-minus-target term; explain unbounded unregularized coefficients, regularization, and numerical stability. |
| P-04 | Advanced | Labels arrive weeks after predictions. How do you detect model degradation without pretending feature drift proves accuracy has dropped? | Separate input drift, output drift, and labeled performance; use proxy alerts carefully; backfill delayed metrics and avoid unsupported causal conclusions. |
| P-05 | Advanced | An experiment improves average engagement but hurts a small user group. How would you investigate and decide whether to launch? | Define the unit of randomization, uncertainty, predeclared guardrails, subgroup sample sizes, practical effect size, and staged rollout criteria. |

## LLM fundamentals and adaptation

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-06 | Foundation | Derive scaled dot-product attention and explain why masking and scaling affect its behavior. | Track Q, K, and V shapes; relate scaling to dot-product variance; mask before softmax; test that future tokens cannot influence causal outputs. |
| P-07 | Applied | An assistant's tool result is larger than the remaining context budget. How should the system decide what to retain? | Count tokens with the actual tokenizer; reserve output capacity; preserve required instructions; summarize or retrieve selectively; detect truncation and test the resulting quality. |
| P-08 | Foundation | Does setting temperature to zero guarantee correct or repeatable output? How would you explain the limitations to a product manager? | Separate sampling behavior from factuality and grounding; distinguish deterministic decoding from reproducibility across model versions and serving implementations. |
| P-09 | Applied | A product needs frequently changing facts and a stable response style. Would you use prompting, retrieval, fine-tuning, or a combination? | Separate knowledge freshness from behavior; establish baselines; consider data rights, maintenance cost, citation needs, and evaluations before choosing. |
| P-10 | Advanced | You plan a low-rank adaptation experiment. How would you estimate trainable parameters and prove it improves the target task without unacceptable regressions? | For a d_out by d_in matrix, a rank-r update uses r × (d_in + d_out) parameters; also budget optimizer state, activations, held-out evaluation, and unrelated-task checks. |

## RAG and retrieval

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-11 | Applied | One corpus contains legal policies, source code, and support conversations. How would you choose retrieval units and chunking strategies? | Preserve useful boundaries and metadata; compare overlap and parent context; test retrieval quality and citation accuracy by document type rather than using one arbitrary chunk size. |
| P-12 | Applied | Semantic search misses exact product codes but handles paraphrases well. What retrieval experiment would you run? | Compare lexical, dense, and hybrid retrieval on query slices; deduplicate candidates; measure recall and latency; evaluate reranking separately. |
| P-13 | Advanced | A new embedding model changes vector dimensions. How do you migrate a large live index safely? | Version model and index together; build and evaluate a parallel index; control freshness; switch traffic gradually; keep a compatible rollback path. |
| P-14 | Advanced | A user's document access is revoked while cached answers and agent memory still contain that document. How do you prevent further disclosure? | Enforce authorization outside the model; scope caches and memory to identity and permissions; invalidate derived content; recheck access before returning results or taking actions. |
| P-15 | Applied | Retrieval recall improves, but final answers get worse. How do you locate the regression? | Freeze other components; inspect context noise and ordering; evaluate retrieval and generation separately; compare oracle context, reranking, and chunking ablations. |

## Agents and tool use

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-16 | Advanced | An agent can read calendars, send emails, and make purchases. Which actions require approval, and where is that policy enforced? | Classify action risk; use scoped credentials and deterministic authorization; validate arguments; require confirmation for sensitive writes; record auditable outcomes. |
| P-17 | Applied | A tool call times out after a payment may already have succeeded. How do you handle retries without charging twice? | Use idempotency keys and durable operation records; distinguish unknown outcome from failure; query status; avoid blind retries and unsupported exactly-once claims. |
| P-18 | Applied | A research agent repeats the same failed search indefinitely. How would you bound its behavior and preserve useful progress? | Limit steps, time, tokens, and spend; detect repeated states; classify failures; checkpoint evidence; provide a stop or human-escalation path. |
| P-19 | Advanced | Design long-term assistant memory that supports correction, deletion, and separation between users. | Define consent and retention; scope records by user and tenant; track provenance; delete derived memories and caches; test retrieval and deletion paths. |
| P-20 | Foundation | How does exposing a tool through MCP differ from giving a model a function-call schema? What remains the application's responsibility? | Distinguish protocol and connection concerns from model-visible schemas and execution; keep authentication, authorization, validation, budgets, and logging explicit. |

## Serving and real-time systems

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-21 | Advanced | Estimate memory for BF16 inference using parameter count, layer count, KV-head count, head dimension, batch size, and cached sequence length. What else needs headroom? | Weights use about 2 bytes per parameter; KV bytes = 2 × layers × batch × sequence length × KV heads × head dimension × bytes per cache element; reserve buffers, activations, and fragmentation. |
| P-22 | Applied | Time to first token regresses while decode throughput stays unchanged. What would you inspect first? | Trace queueing, retrieval, network, prompt length, and prefill separately; compare request distributions and deployment changes; use tail latency, not only averages. |
| P-23 | Advanced | Compare static and continuous batching under mixed short and long requests. How do you protect small requests from starvation? | Explain admission and completion behavior, KV-cache pressure, scheduling fairness, cancellation, backpressure, and throughput-versus-tail-latency trade-offs. |
| P-24 | Applied | A quantized model is cheaper in memory but slower on your workload. How would you decide whether to deploy it? | Benchmark on the actual hardware and concurrency; inspect kernels and dequantization overhead; measure quality, memory, latency, throughput, and total cost. |
| P-25 | Advanced | Design a voice assistant with an explicit sub-second first-audio budget and safe interruption handling. | Budget audio capture, end-of-turn detection, ASR, retrieval, LLM startup, and TTS; overlap streaming work; handle barge-in, cancellation, failure, and irreversible tool actions. |

## Evaluation, safety, and operations

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-26 | Applied | How would you evaluate whether an assistant's claims are supported by its citations, rather than merely checking that citations exist? | Use claim-level evidence review, citation correctness and coverage, representative negative cases, human labels, and separate answer usefulness from support. |
| P-27 | Advanced | An LLM judge favors longer answers and its scores shift after an update. How do you keep the evaluation trustworthy? | Blind and randomize comparisons; control rubrics and versions; compare against human judgments; test position and length biases; retain raw outputs and uncertainty. |
| P-28 | Advanced | A retrieved PDF contains instructions to export confidential data. How should the system treat this content? | Treat retrieved text as untrusted data; enforce tool permissions and destination rules outside the model; constrain sensitive actions; test indirect injection and information-flow failures. |
| P-29 | Applied | You need useful production traces without storing raw private prompts. What telemetry would you collect? | Minimize data; collect timings, versions, statuses, costs, and scoped identifiers; redact sensitive fields; control access, sampling, retention, and trace visibility. |
| P-30 | Advanced | A prompt, model, and retrieval index all change in one release. How do you evaluate and roll back safely? | Pin component versions; compare controlled variants; gate on quality and safety slices; canary traffic; monitor online outcomes; retain a mutually compatible rollback bundle. |

## Python, SQL, and testing

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-31 | Applied | Write an async LLM client with bounded concurrency, retryable-error handling, and an overall request deadline. | Use a semaphore or equivalent; honor cancellation and retry-after; apply bounded backoff with jitter; distinguish permanent failures; test timeouts without real API charges. |
| P-32 | Applied | Select useful retrieved passages under a token budget while preserving complete citation spans. How do you test the selector? | Define utility, overlap, and ordering; count actual tokens; compare a simple greedy baseline with alternatives; test empty input, oversized spans, ties, and the budget invariant. |
| P-33 | Applied | Implement top-k cosine-similarity search over a stream of vectors. What assumptions and edge cases matter? | Validate dimensions and zero norms; keep a bounded heap; define tie behavior; analyze time and memory; test against a brute-force reference. |
| P-34 | Applied | Given request logs with tenant_id, event_time, latency_ms, tokens, and status, write SQL for daily per-tenant error rate and p95 latency. | Define error statuses and timezone; handle nulls and zero counts; choose the database's percentile function; separate successful-request latency if required. |
| P-35 | Applied | Test an agent that uses a nondeterministic model and an external ticketing API. Which assertions should be exact? | Mock transport and tool calls; assert schemas, permissions, budgets, and state transitions exactly; use rubrics for text quality; test retries, cancellation, and duplicate writes. |

## Behavioral and project discussion

| ID | Depth | Original practice question | Answer checklist |
| --- | --- | --- | --- |
| P-36 | Applied | Explain an AI feature you shipped, the user problem, and how you measured its impact after launch. | Use your own experience; separate baseline and actual results; describe your contribution, evaluation, rollout, and limitations. |
| P-37 | Applied | Tell us about a data or modeling assumption that proved wrong. What did you change? | Explain the evidence, responsibility, correction, stakeholder communication, and prevention; do not hide the mistake behind team-wide language. |
| P-38 | Applied | Describe a disagreement about whether to fine-tune a model or improve retrieval. How did you resolve it? | State both viewpoints fairly; compare experiments, constraints, cost, and decision ownership; show the outcome and your learning. |
| P-39 | Advanced | A launch date is fixed, but quality and latency goals are not met. How would you renegotiate scope? | Identify non-negotiable safety gates; propose a narrower launch or fallback; communicate evidence, ownership, residual risk, and rollback options. |
| P-40 | Applied | How would you assess a promising new AI technique before recommending it for production? | Read limitations; reproduce a relevant baseline; test on representative data; include engineering cost, licensing, privacy, and a clear adoption criterion. |

## Self-review

For each answer, ask:

- Did I clarify the problem before choosing a technology?
- Can I explain a simple baseline and at least one alternative?
- Did I define measurable quality, latency, and cost goals?
- Did I account for missing data, permissions, and partial failures?
- Can I test the implementation or validate the design experimentally?
- Did I distinguish what I know from what I still need to verify?

Use this checklist to identify gaps, not as a prediction of hiring outcomes.
