# Company-wise reported interview questions

[Home](../README.md) · [Practice bank](practice-questions.md) ·
[Source notes](sources.md)

**Research cutoff: September 30, 2026.** These are paraphrases of public reports,
not complete interview specifications or a real-time feed.

**C** = full candidate report reviewed; **I** = search-index excerpt only;
**S** = secondary compilation. All three are reports, not employer-confirmed
facts. Each row identifies its evidence and links to its source.

The **preparation pointers are our own advice**, not additional questions
reported by candidates or an employer's official answer key. Ask for missing
constraints before solving an incomplete prompt.

## Amazon

**Reported role:** Machine Learning Engineer. **Evidence:** I; full-page access
was blocked. Interview dates for these particular items were not established.
Source: Glassdoor candidate-report excerpts
[1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm).

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| AMZ-01 | Describe your most impactful recent project and how it worked end to end. | Explain your contribution, architecture, measurable outcomes, and operational failures. | I · [1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm) |
| AMZ-02 | How do you systematically evaluate and improve prompts in a complex LLM workflow? | Version prompts; use representative held-out cases; compare quality, cost, and latency. | I · [1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm) |
| AMZ-03 | What online or live evaluations did you run before deployment, beyond offline tests? | Distinguish shadow testing, controlled experiments, safety gates, and rollback criteria. | I · [1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm) |
| AMZ-04 | Compare encoder-only and decoder-only model architectures. | Discuss masking, representations, training objectives, and task suitability. | I · [1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm) |
| AMZ-05 | Write and explain the mathematical form of cross-entropy loss. | Define the target distribution and predicted probabilities; explain masking and stable computation. | I · [1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm) |
| AMZ-06 | How do parallel attention heads operate, and how are their outputs combined? | Track tensor shapes, head concatenation, output projection, and the following network block. | I · [1](https://www.glassdoor.com/Interview/Amazon-Machine-Learning-Engineer-Interview-Questions-EI_IE6036.0,6_KO7,32.htm) |

## Anthropic

**Reported roles:** ANT-01–05 are from a senior Software Engineer, Safeguards
report [8](https://www.tryexponent.com/experiences/anthropic-senior-software-engineer-interview-2ffa5f).
ANT-06 is a serving question without a specific role attached; ANT-07 is
attributed to an ML engineer in a compilation
[9](https://www.tryexponent.com/blog/ai-engineer-interview-questions).
The candidate page displays a relative interview date, not an absolute date.

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| ANT-01 | Turn sampled call stacks into an execution trace. | Clarify event ordering and stack semantics; test recursion, repeated frames, and transitions. | C · [8](https://www.tryexponent.com/experiences/anthropic-senior-software-engineer-interview-2ffa5f) |
| ANT-02 | Design an API for efficient LLM sampling with batching and request orchestration. | Define cancellation, queue limits, fairness, latency budgets, and result routing. | C · [8](https://www.tryexponent.com/experiences/anthropic-senior-software-engineer-interview-2ffa5f) |
| ANT-03 | Implement an LRU cache, then discuss file handling and serialization trade-offs. | Start with an explicit cache contract; separate eviction from persistence; test failed writes. | C · [8](https://www.tryexponent.com/experiences/anthropic-senior-software-engineer-interview-2ffa5f) |
| ANT-04 | Discuss work that conflicted with your values and how you felt then and afterward. | Use a truthful example; explain responsibility, escalation, reflection, and changes in behavior. | C · [8](https://www.tryexponent.com/experiences/anthropic-senior-software-engineer-interview-2ffa5f) |
| ANT-05 | Tell a story about believing you had a solution and later discovering you were wrong. | Explain the evidence that changed your mind and how you corrected the outcome. | C · [8](https://www.tryexponent.com/experiences/anthropic-senior-software-engineer-interview-2ffa5f) |
| ANT-06 | Schedule synchronous inference requests on one GPU with batches capped at 100 inputs. | Compare batch-size and wait-time triggers; account for variable input sizes and backpressure. | S · [9](https://www.tryexponent.com/blog/ai-engineer-interview-questions) |
| ANT-07 | Investigate a conversational model that confidently gives wrong answers in high-risk use. | Separate correctness from confidence; test grounding, abstention, escalation, and residual risk. | S · [9](https://www.tryexponent.com/blog/ai-engineer-interview-questions) |

## Databricks

**Reported role:** Unspecified. **Evidence:** S only. The compilation attributes
this scenario to Databricks but does not expose the interview's date, team, or
level [9](https://www.tryexponent.com/blog/ai-engineer-interview-questions).
Do not read the preparation pointers as reported Databricks follow-ups.

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| DBX-01 | Design a pipeline that ingests and indexes a large, mixed-format document collection for LLM use. | Discuss parsing, chunk IDs, incremental updates, deduplication, access control, and index rollback. | S · [9](https://www.tryexponent.com/blog/ai-engineer-interview-questions) |

For additional exercises without a company attribution, use
[RAG and retrieval](practice-questions.md#rag-and-retrieval) and
[serving and real-time systems](practice-questions.md#serving-and-real-time-systems).

## Glean

**Reported role:** New-grad engineering; this is not explicitly an AI Engineer
report. **Evidence:** C. The post is dated March 2, 2025, with a March 3 update;
the interview date itself is not stated
[7](https://leetcode.com/discuss/interview-question/6487496/Glean-or-New-Grad-Interview-Full-Loop-Experience/).
Several original specifications are incomplete.

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| GLN-01 | Find the kth-largest element across m arrays, each of size n. | Ask whether arrays are sorted and how duplicates count; compare selection and heap strategies. | C · [7](https://leetcode.com/discuss/interview-question/6487496/Glean-or-New-Grad-Interview-Full-Loop-Experience/) |
| GLN-02 | Prove why a median is the best balancing value for an array. | The report omits the objective; clarify it before using an absolute-deviation argument. | C · [7](https://leetcode.com/discuss/interview-question/6487496/Glean-or-New-Grad-Interview-Full-Loop-Experience/) |
| GLN-03 | Build a table module, progressing from row and column operations to joins. | Define schemas, types, join behavior, and tests; do not assume the unreported later stages. | C · [7](https://leetcode.com/discuss/interview-question/6487496/Glean-or-New-Grad-Interview-Full-Loop-Experience/) |
| GLN-04 | Restore sorted order when one array element is out of place. | Clarify duplicates and mutation rules; find the misplaced item and its insertion point. | C · [7](https://leetcode.com/discuss/interview-question/6487496/Glean-or-New-Grad-Interview-Full-Loop-Experience/) |

## Google

**Reported roles:** GOO-01–05 come from an AI Engineer Intern report
[1](https://www.tryexponent.com/experiences/google-machine-learning-engineer-intern-interview-d43fed).
GOO-06–07 come from Machine Learning Engineer excerpts
[2](https://www.glassdoor.com/Interview/Google-Machine-Learning-Engineer-Interview-Questions-EI_IE9079.0,6_KO7,32.htm).
The intern page uses a relative interview date; dates for the excerpted MLE
questions were not established. Do not generalize an internship loop to all
Google AI roles.

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| GOO-01 | Return a palindromic subsequence from a string. | The objective is incomplete; ask whether a longest subsequence is required and how ties work. | C · [1](https://www.tryexponent.com/experiences/google-machine-learning-engineer-intern-interview-d43fed) |
| GOO-02 | Compute a binary tree's diameter. | Define diameter in edges or nodes; derive a post-order solution and test skewed trees. | C · [1](https://www.tryexponent.com/experiences/google-machine-learning-engineer-intern-interview-d43fed) |
| GOO-03 | Solve a House Robber-style dynamic-programming problem. | Clarify the variant; state the recurrence, base cases, and space optimization. | C · [1](https://www.tryexponent.com/experiences/google-machine-learning-engineer-intern-interview-d43fed) |
| GOO-04 | Find the number of coins needed to make a target sum. | Confirm whether the count must be minimal and whether reuse is allowed; handle impossible targets. | C · [1](https://www.tryexponent.com/experiences/google-machine-learning-engineer-intern-interview-d43fed) |
| GOO-05 | Explain your projects and previous experience. | Distinguish your work from the team's work and defend decisions with evidence. | C · [1](https://www.tryexponent.com/experiences/google-machine-learning-engineer-intern-interview-d43fed) |
| GOO-06 | Implement a multi-head attention class and discuss model-related questions. | Explain tensor dimensions, causal masks, projections, and numerical tests. | I · [2](https://www.glassdoor.com/Interview/Google-Machine-Learning-Engineer-Interview-Questions-EI_IE9079.0,6_KO7,32.htm) |
| GOO-07 | What is the time complexity of training a support vector machine? | Specify the solver, kernel, sample count, feature dimension, and assumptions; avoid one universal bound. | I · [2](https://www.glassdoor.com/Interview/Google-Machine-Learning-Engineer-Interview-Questions-EI_IE9079.0,6_KO7,32.htm) |

## Meta

**Reported role:** Machine Learning Engineer. **Evidence:** I; full-page access
was blocked. The index includes dated 2025 and January 2026 reports, but that
does not establish interview dates for every row
[5](https://www.glassdoor.co.in/Interview/Meta-Machine-Learning-Engineer-Interview-Questions-EI_IE40772.0,4_KO5,30.htm).

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| MET-01 | Merge three sorted lists into one sorted list. | Clarify arrays versus linked lists; compare pointer and heap solutions; test duplicates. | I · [5](https://www.glassdoor.co.in/Interview/Meta-Machine-Learning-Engineer-Interview-Questions-EI_IE40772.0,4_KO5,30.htm) |
| MET-02 | Design a short-video recommendation system like TikTok's. | Separate candidate generation and ranking; discuss feedback bias, freshness, diversity, and serving. | I · [5](https://www.glassdoor.co.in/Interview/Meta-Machine-Learning-Engineer-Interview-Questions-EI_IE40772.0,4_KO5,30.htm) |
| MET-03 | Design a news-feed ranking system. | Start with product objectives; explain labels, temporal splits, experimentation, and safeguards. | I · [5](https://www.glassdoor.co.in/Interview/Meta-Machine-Learning-Engineer-Interview-Questions-EI_IE40772.0,4_KO5,30.htm) |
| MET-04 | Implement an LRU cache. | Define get/put semantics; aim for constant-time operations; test updates and eviction. | I · [5](https://www.glassdoor.co.in/Interview/Meta-Machine-Learning-Engineer-Interview-Questions-EI_IE40772.0,4_KO5,30.htm) |

## Microsoft

**Reported role:** Machine Learning Engineer. **Evidence:** I; full-page access
was blocked. This is historical foundational material: the indexed page shows
an October 2, 2024 update, not a 2026 interview date
[3](https://www.glassdoor.ca/Interview/Microsoft-Machine-Learning-Engineer-Interview-Questions-EI_IE1651.0,9_KO10,35.htm).

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| MSF-01 | Compare L1- and L2-regularized regression. | Discuss penalty geometry, sparsity, correlated features, and the regularization parameter. | I · [3](https://www.glassdoor.ca/Interview/Microsoft-Machine-Learning-Engineer-Interview-Questions-EI_IE1651.0,9_KO10,35.htm) |
| MSF-02 | Draw BERT's architecture and explain it. | Cover bidirectional attention, embeddings, pretraining, and task-specific heads. | I · [3](https://www.glassdoor.ca/Interview/Microsoft-Machine-Learning-Engineer-Interview-Questions-EI_IE1651.0,9_KO10,35.htm) |
| MSF-03 | Solve the Number of Islands grid problem. | Clarify connectivity and mutation; compare DFS, BFS, and union-find; test boundaries. | I · [3](https://www.glassdoor.ca/Interview/Microsoft-Machine-Learning-Engineer-Interview-Questions-EI_IE1651.0,9_KO10,35.htm) |
| MSF-04 | How would you choose k for K-means? | Combine stability, internal metrics, downstream utility, and the algorithm's assumptions. | I · [3](https://www.glassdoor.ca/Interview/Microsoft-Machine-Learning-Engineer-Interview-Questions-EI_IE1651.0,9_KO10,35.htm) |

## NVIDIA

**Reported role:** AI Engineer. **Evidence:** I; full-page access was blocked.
These are three parts of one reported technical prompt, not three independent
interviews. The interview date was not established
[4](https://www.glassdoor.com/Interview/NVIDIA-AI-Engineer-Interview-Questions-EI_IE7633.0,6_KO7,18.htm).

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| NVD-01 | What makes an algorithm numerically unstable? | Distinguish algorithmic stability from problem conditioning; explain rounding and cancellation. | I · [4](https://www.glassdoor.com/Interview/NVIDIA-AI-Engineer-Interview-Questions-EI_IE7633.0,6_KO7,18.htm) |
| NVD-02 | Compare FP32, FP16, and BF16 for training large models on GPUs. | Discuss range, precision, mixed precision, accumulation, overflow, and hardware support. | I · [4](https://www.glassdoor.com/Interview/NVIDIA-AI-Engineer-Interview-Questions-EI_IE7633.0,6_KO7,18.htm) |
| NVD-03 | When would sparse matrices beat dense storage on a large dataset, especially for bandwidth? | Include density, index overhead, memory access patterns, kernel support, and actual profiling. | I · [4](https://www.glassdoor.com/Interview/NVIDIA-AI-Engineer-Interview-Questions-EI_IE7633.0,6_KO7,18.htm) |

## OpenAI

**Reported role:** Senior Full Stack Engineer, Applied. **Evidence:** C. This
is an applied product-engineering report, not a research scientist loop. The
page displays a relative interview date
[5](https://www.tryexponent.com/experiences/openai-software-engineer-interview-5a7d13).

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| OAI-01 | Design a developer playground, covering its UI and underlying behavior. | Clarify whether model serving is in scope; cover conversation state, experiments, and streaming. | C · [5](https://www.tryexponent.com/experiences/openai-software-engineer-interview-5a7d13) |
| OAI-02 | Compute a user's remaining credits from grants, usage, and expiration rules. | Define event ordering and boundary semantics; test overlapping grants, expiration, and invalid usage. | C · [5](https://www.tryexponent.com/experiences/openai-software-engineer-interview-5a7d13) |
| OAI-03 | Refactor a messy codebase to meet new requirements while preserving passing tests. | Identify invariants, introduce small abstractions, and use regression tests before restructuring. | C · [5](https://www.tryexponent.com/experiences/openai-software-engineer-interview-5a7d13) |
| OAI-04 | Present a challenging project and defend storage, LLM, evaluation, and scaling decisions. | Explain alternatives, actual scale, evaluation evidence, and your personal contribution. | C · [5](https://www.tryexponent.com/experiences/openai-software-engineer-interview-5a7d13) |
| OAI-05 | Describe a conflict and how you resolved it. | Show the disagreement, your actions, the outcome, and what you learned. | C · [5](https://www.tryexponent.com/experiences/openai-software-engineer-interview-5a7d13) |

## Scale AI

**Reported role:** New Grad Software Engineer. **Evidence:** C. The report
includes practical AI backend scenarios, but the job title is SWE. The page
displays a relative interview date
[2](https://www.tryexponent.com/experiences/scale-ai-software-engineer-interview-b83485).

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| SCL-01 | Find free ingestion windows among query intervals supplied through a method or API. | Clarify interval endpoints; normalize the input; merge occupied windows and find gaps. | C · [2](https://www.tryexponent.com/experiences/scale-ai-software-engineer-interview-b83485) |
| SCL-02 | Design an insurance-claims backend using documents and RAG while controlling LLM cost. | Explain ingestion, policy retrieval, decision auditing, token budgets, and human review. | C · [2](https://www.tryexponent.com/experiences/scale-ai-software-engineer-interview-b83485) |
| SCL-03 | Debug a codebase that is failing to produce model output. | Trace the relevant methods; isolate API, parsing, async, and configuration failures with tests. | C · [2](https://www.tryexponent.com/experiences/scale-ai-software-engineer-interview-b83485) |
| SCL-04 | Determine which meeting rooms are free and when, using a supplied input method. | Clarify time ranges and room identifiers; handle overlaps, empty schedules, and ties. | C · [2](https://www.tryexponent.com/experiences/scale-ai-software-engineer-interview-b83485) |

## Sierra AI

**Reported role:** Agent Engineer. **Evidence:** C. The page displays a
relative interview date
[1](https://www.tryexponent.com/experiences/sierra-ai-machine-learning-engineer-interview-8549fc).
The candidate chose RAG as a teaching topic; this is not evidence that an
interviewer specifically required a RAG explanation.

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| SIE-01 | Detect circular references among cells in an Excel-like spreadsheet. | Model dependencies as a graph; distinguish visited nodes from the active DFS path. | C · [1](https://www.tryexponent.com/experiences/sierra-ai-machine-learning-engineer-interview-8549fc) |
| SIE-02 | Build a support agent for a fictional outdoors company and choose two of five features. | Explain prioritization, tool contracts, failure handling, and testable completion criteria. | C · [1](https://www.tryexponent.com/experiences/sierra-ai-machine-learning-engineer-interview-8549fc) |
| SIE-03 | Debug several issues in an existing TypeScript and React implementation. | Read the code and reproduce failures; understand state, async behavior, and regression tests. | C · [1](https://www.tryexponent.com/experiences/sierra-ai-machine-learning-engineer-interview-8549fc) |
| SIE-04 | Explain your agent's design choices and which production metrics and dashboards you would use. | Discuss task success, unsafe actions, handoffs, latency, cost, and traces without leaking private data. | C · [1](https://www.tryexponent.com/experiences/sierra-ai-machine-learning-engineer-interview-8549fc) |

## xAI

**Reported role:** Entry-level Member of Technical Staff. **Evidence:** C. The
page displays a relative interview date. The report's AI-tool allowance is
specific to that candidate's exercise, not a universal interview policy
[1](https://www.tryexponent.com/experiences/x-ai-software-engineer-interview-7fbd12).

| ID | Paraphrased reported question | Preparation pointers | Evidence and source |
| --- | --- | --- | --- |
| XAI-01 | Select one supplied prompt and build a demoable product within four hours. | Deliver a narrow working slice; document setup, mock external dependencies, and explain limitations. | C · [1](https://www.tryexponent.com/experiences/x-ai-software-engineer-interview-7fbd12) |
| XAI-02 | Complete functionality in an unfamiliar class that queues token-sized LLM inputs and returns outputs. | First trace data flow and invariants; clarify queue behavior, ordering, and expected results. | C · [1](https://www.tryexponent.com/experiences/x-ai-software-engineer-interview-7fbd12) |
| XAI-03 | Explain your technical background and which product or research area you want to work on. | Connect real experience to a specific area without overstating your contribution. | C · [1](https://www.tryexponent.com/experiences/x-ai-software-engineer-interview-7fbd12) |

## Coverage gaps

This collection intentionally does not label generic questions as “asked at”
companies for which no suitable report was reviewed. A single report also does
not establish how often a question is asked. Other companies and additional
roles can be added through the [contribution process](../CONTRIBUTING.md).

If a source disappears, becomes access-restricted, or changes its wording,
update its [source note](sources.md) and reconsider the evidence label. Preserve
uncertainty rather than filling missing details with guesses.
