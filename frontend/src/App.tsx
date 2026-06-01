import { useEffect, useRef, useState } from "react";
import { NavLink, Navigate, Route, Routes } from "react-router";
import { accountRestrictionMessage, ApiError, loginUrl, logout, unwrap } from "./api";
import {
  createPronunciation,
  getCurrentUser,
  getDictionaryReview,
  getPronunciation,
  getPronunciationVideo,
  getWordInfo,
  getWordSuggestions,
  listAdminUsers,
  submitDictionaryReview,
  updateAdminUserStatus
} from "./api/generated/sdk.gen";
import type {
  AdminUserResponse,
  CurrentUserResponse,
  DictionaryReviewItem,
  DictionaryReviewSubmitResponse,
  UserStatus,
  WordInfoResponse,
  WordSuggestion
} from "./api/generated/types.gen";

type AuthState = "checking" | "signed-out" | "signed-in";

export default function App() {
  const [authState, setAuthState] = useState<AuthState>("checking");
  const [currentUser, setCurrentUser] = useState<CurrentUserResponse>();

  async function reloadCurrentUser() {
    setAuthState("checking");
    try {
      const user = await unwrap(getCurrentUser());
      setCurrentUser(user);
      setAuthState("signed-in");
    }
    catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setCurrentUser(undefined);
        setAuthState("signed-out");
        return;
      }
      setCurrentUser(undefined);
      setAuthState("signed-out");
    }
  }

  async function logoutCurrentUser() {
    await logout();
    setCurrentUser(undefined);
    setAuthState("signed-out");
  }

  function handleAuthError(error: unknown) {
    if (error instanceof ApiError && error.status === 401) {
      setCurrentUser(undefined);
      setAuthState("signed-out");
    }
  }

  useEffect(() => {
    void reloadCurrentUser();
  }, []);

  return (
    <div className="app-shell">
      <header className="topbar">
        <nav className="nav-links" aria-label="Main navigation">
          <NavLink to="/" end>Home</NavLink>
          <NavLink to="/add">Add</NavLink>
          <NavLink to="/review">Review</NavLink>
          {currentUser?.admin ? <NavLink to="/admin">Admin</NavLink> : null}
        </nav>
        <AuthPanel authState={authState} user={currentUser} onLogout={logoutCurrentUser} />
      </header>

      <Routes>
        <Route path="/" element={<HomePage user={currentUser} authState={authState} />} />
        <Route path="/add" element={<AddWordPage user={currentUser} authState={authState} onAuthError={handleAuthError} />} />
        <Route path="/review" element={<ReviewPage user={currentUser} authState={authState} onAuthError={handleAuthError} />} />
        <Route path="/admin" element={<AdminPage user={currentUser} authState={authState} onAuthError={handleAuthError} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}

function AuthPanel({ authState, user, onLogout }: { authState: AuthState; user?: CurrentUserResponse; onLogout: () => Promise<void> }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submitLogout() {
    setBusy(true);
    setError("");
    try {
      await onLogout();
    }
    catch (logoutError) {
      setError(logoutError instanceof Error ? logoutError.message : "Could not log out.");
    }
    finally {
      setBusy(false);
    }
  }

  if (authState === "checking") {
    return <div className="auth-panel subtle">Checking sign-in...</div>;
  }
  if (!user) {
    return <a className="login-button" href={loginUrl()}><img src="/google-g.svg" alt="" aria-hidden="true" />Sign in</a>;
  }
  return (
    <div className="auth-panel">
      <div>
        <strong>{user.displayName}</strong>
        <small>{user.email} · {user.status}</small>
        {error ? <small className="error-text">{error}</small> : null}
      </div>
      <button type="button" className="secondary small" disabled={busy} onClick={submitLogout}>Logout</button>
    </div>
  );
}

function HomePage({ user, authState }: { user?: CurrentUserResponse; authState: AuthState }) {
  return (
    <main className="home-layout">
      <section className="hero-card home-hero">
        <p className="eyebrow">Vocavista PWA</p>
        <h1>German words that fit in your pocket.</h1>
        <p>Add vocabulary with generated pronunciation video, then review it from your phone whenever you have a minute.</p>
        {authState === "signed-out" ? <a className="login-button wide" href={loginUrl()}><img src="/google-g.svg" alt="" aria-hidden="true" />Sign in with Google</a> : null}
        {user && !user.functionalAccessAllowed ? <AccountNotice user={user} /> : null}
      </section>
      <section className="action-grid" aria-label="Primary actions">
        <NavLink className="action-card add-card" to="/add">
          <span>Add word</span>
          <strong>Search, choose a phrase, generate video.</strong>
        </NavLink>
        <NavLink className="action-card review-card-link" to="/review">
          <span>Review</span>
          <strong>Recall from English and Russian prompts.</strong>
        </NavLink>
      </section>
    </main>
  );
}

function AddWordPage({ user, authState, onAuthError }: PageProps) {
  const [word, setWord] = useState("Hausaufgabe");
  const [phrase, setPhrase] = useState("Ich mache meine Hausaufgabe nach dem Abendessen.");
  const [status, setStatus] = useState("Sign in with Google to use this page.");
  const [suggestions, setSuggestions] = useState<WordSuggestion[]>([]);
  const [wordInfo, setWordInfo] = useState<WordInfoResponse>();
  const [wordInfoId, setWordInfoId] = useState<string>();
  const [videoUrl, setVideoUrl] = useState<string>();
  const [searchBusy, setSearchBusy] = useState(false);
  const [generateBusy, setGenerateBusy] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const canUseFeatures = Boolean(user?.functionalAccessAllowed);

  useEffect(() => {
    if (authState === "signed-out") {
      setStatus("Sign in with Google to use this page.");
    }
    else if (user && !user.functionalAccessAllowed) {
      setStatus(accountRestrictionMessage(user.status));
    }
    else if (user) {
      setStatus("Search a word first, choose an example phrase, then generate video.");
    }
  }, [authState, user]);

  useEffect(() => {
    if (!user || word.trim().length < 2) {
      setSuggestions([]);
      return;
    }
    const timeout = window.setTimeout(() => {
      unwrap(getWordSuggestions({ query: { query: word.trim() } }))
        .then((data) => setSuggestions(data.items))
        .catch((error: unknown) => {
          onAuthError(error);
          setStatus(error instanceof Error ? error.message : "Could not load suggestions.");
        });
    }, 250);
    return () => window.clearTimeout(timeout);
  }, [onAuthError, user, word]);

  async function loadWordInfo() {
    if (!canUseFeatures) {
      setStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to use this page.");
      return;
    }
    const trimmedWord = word.trim();
    if (!trimmedWord) {
      setStatus("Enter a word first.");
      return;
    }
    setSearchBusy(true);
    setWordInfo(undefined);
    try {
      setStatus("Loading word info...");
      const info = await unwrap(getWordInfo({ query: { word: trimmedWord } }));
      setWordInfo(info);
      setWordInfoId(info.id);
      setWord(info.normalizedWord);
      setStatus("Choose an example phrase, then generate Veo video.");
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not load word info.");
    }
    finally {
      setSearchBusy(false);
    }
  }

  function selectSuggestion(suggestion: WordSuggestion) {
    setSuggestions([]);
    setWord(suggestion.word);
    setWordInfoId(suggestion.wordInfoId ?? undefined);
    if (suggestion.phrase) {
      setPhrase(suggestion.phrase);
    }
    if (suggestion.videoUrl) {
      setVideoUrl(suggestion.videoUrl);
      setStatus("Selected an existing pronunciation video. Generate will reuse it.");
      return;
    }
    setStatus("Selected existing entry. Search word info or generate a new video when ready.");
  }

  async function generateVideo() {
    if (!canUseFeatures) {
      setStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to use this page.");
      return;
    }
    if (!wordInfoId) {
      setStatus("Search word info before generating video.");
      return;
    }
    setGenerateBusy(true);
    try {
      setStatus("Queueing video generation...");
      const queued = await unwrap(createPronunciation({ body: { wordInfoId, word, phrase, language: "de" } }));
      const completed = await pollPronunciation(queued.id, setStatus);
      if (completed.status === "failed") {
        throw new Error(`${completed.errorCode ?? "generation_failed"}: ${completed.errorMessage ?? "Video generation failed."}`);
      }
      if (!completed.videoUrl) {
        throw new Error("Generation completed without videoUrl.");
      }
      setVideoUrl(completed.videoUrl);
      setStatus(`Playing generated video. id=${completed.id}`);
      window.setTimeout(() => void videoRef.current?.play(), 50);
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not generate video.");
    }
    finally {
      setGenerateBusy(false);
    }
  }

  return (
    <main className="mobile-workspace add-layout">
      <section className="panel controls-panel">
        <p className="eyebrow">Add word</p>
        <h1>Make a tiny pronunciation lesson.</h1>
        <p>Search a German word, pick an example sentence, and generate a vertical lip-sync video for mobile review.</p>
        {user && !user.functionalAccessAllowed ? <AccountNotice user={user} /> : null}
        {authState === "signed-out" ? <SignInCard message="Sign in with Google to search and generate pronunciation video." /> : null}

        <label>
          Word
          <input value={word} onChange={(event) => { setWordInfoId(undefined); setWord(event.target.value); }} autoComplete="off" />
        </label>

        <div className="suggestions">
          {suggestions.map((suggestion) => (
            <button key={`${suggestion.source}-${suggestion.word}-${suggestion.phrase ?? ""}`} type="button" className="soft-list-button" disabled={!canUseFeatures} onClick={() => selectSuggestion(suggestion)}>
              <strong>{suggestion.word}</strong>
              <small>{describeSuggestion(suggestion)}</small>
            </button>
          ))}
        </div>

        <button type="button" className="secondary" disabled={!canUseFeatures || searchBusy} onClick={loadWordInfo}>Search word</button>

        {wordInfo ? <WordInfoPanel info={wordInfo} onUseWord={() => { setWord(wordInfo.normalizedWord); setWordInfoId(wordInfo.id); setStatus(`Using normalized word: ${wordInfo.normalizedWord}`); }} onUsePhrase={(sentence) => { setWord(wordInfo.normalizedWord); setPhrase(sentence); setStatus(`Phrase selected for ${wordInfo.normalizedWord}.`); }} /> : null}

        <label>
          Phrase
          <textarea value={phrase} onChange={(event) => setPhrase(event.target.value)} />
        </label>
        <button type="button" disabled={!canUseFeatures || generateBusy} onClick={generateVideo}>Generate video</button>
        <StatusBox>{status}</StatusBox>
      </section>

      <section className="video-stage">
        {videoUrl ? <video ref={videoRef} src={videoUrl} controls playsInline /> : <div className="placeholder">Generated MP4 video will appear here when Veo completes.</div>}
      </section>
    </main>
  );
}

function ReviewPage({ user, authState, onAuthError }: PageProps) {
  const [status, setStatus] = useState("Sign in to start reviewing.");
  const [items, setItems] = useState<DictionaryReviewItem[]>([]);
  const [index, setIndex] = useState(0);
  const [answer, setAnswer] = useState("");
  const [answered, setAnswered] = useState(false);
  const [result, setResult] = useState<DictionaryReviewSubmitResponse>();
  const [includeUpcoming, setIncludeUpcoming] = useState(false);
  const canUseFeatures = Boolean(user?.functionalAccessAllowed);
  const item = items[index];

  useEffect(() => {
    if (authState === "signed-out") {
      setStatus("Sign in with Google to review words.");
    }
    else if (user && !user.functionalAccessAllowed) {
      setStatus(accountRestrictionMessage(user.status));
    }
    else if (user) {
      void loadBatch(false);
    }
  }, [authState, user]);

  async function loadBatch(nextIncludeUpcoming: boolean) {
    if (!canUseFeatures) {
      setStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to review words.");
      return;
    }
    try {
      const data = await unwrap(getDictionaryReview({ query: { limit: 10, includeUpcoming: nextIncludeUpcoming } }));
      setItems(data.items);
      setIndex(0);
      setAnswer("");
      setAnswered(false);
      setResult(undefined);
      setIncludeUpcoming(nextIncludeUpcoming);
      setStatus(data.items.length === 0 ? "No due words right now." : nextIncludeUpcoming ? "Practicing the next words by due date." : "Review due words.");
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not load review words.");
    }
  }

  async function recordResult(correct: boolean) {
    if (!item || answered) {
      return;
    }
    setAnswered(true);
    try {
      const response = await unwrap(submitDictionaryReview({ path: { entryId: item.entryId }, body: { correct } }));
      setResult(response);
      setStatus("Answer revealed.");
    }
    catch (error) {
      setAnswered(false);
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not submit review.");
    }
  }

  function nextItem() {
    const nextIndex = index + 1;
    if (nextIndex >= items.length) {
      if (includeUpcoming) {
        setItems([]);
        setStatus("Finished practice batch.");
        return;
      }
      void loadBatch(false);
      return;
    }
    setIndex(nextIndex);
    setAnswer("");
    setAnswered(false);
    setResult(undefined);
  }

  useEffect(() => {
    if (item && !answered && normalizeAnswer(answer) === normalizeAnswer(item.expectedAnswer)) {
      void recordResult(true);
    }
  }, [answer, answered, item]);

  return (
    <main className="mobile-workspace review-layout">
      <section className="panel controls-panel">
        <p className="eyebrow">Review</p>
        <h1>Recall the German word.</h1>
        <p>Use the English and Russian prompts, type the German answer, and include the article for nouns.</p>
        {user && !user.functionalAccessAllowed ? <AccountNotice user={user} /> : null}
        {authState === "signed-out" ? <SignInCard message="Sign in with Google to review your dictionary." /> : null}
        <button type="button" disabled={!canUseFeatures} onClick={() => void loadBatch(false)}>Load due words</button>
        <button type="button" className="secondary" disabled={!canUseFeatures} onClick={() => void loadBatch(true)}>Practice more</button>
        <StatusBox>{status}</StatusBox>
      </section>

      <section className="review-stage">
        {!item ? <DoneCard /> : (
          <article className="study-card">
            <p className="meta">Item {index + 1} of {items.length} · {item.partOfSpeech}{item.article ? ` · ${item.article}` : ""}</p>
            <div className="prompt-grid">
              <Prompt label="English" value={joinText(item.translations.en) || "No English translation"} />
              <Prompt label="Russian" value={joinText(item.translations.ru) || "No Russian translation"} />
            </div>
            <label>
              German answer
              <input value={answer} onChange={(event) => setAnswer(event.target.value)} disabled={answered} autoComplete="off" autoFocus />
            </label>
            <button type="button" className="danger" disabled={answered} onClick={() => void recordResult(false)}>I do not remember</button>
            {result ? <ReviewResult item={item} result={result} onNext={nextItem} finalItem={index + 1 >= items.length} onAuthError={onAuthError} /> : null}
          </article>
        )}
      </section>
    </main>
  );
}

function AdminPage({ user, authState, onAuthError }: PageProps) {
  const [status, setStatus] = useState("Checking admin access...");
  const [users, setUsers] = useState<AdminUserResponse[]>([]);

  useEffect(() => {
    if (authState === "signed-out") {
      setStatus("Sign in with an admin Google account to manage users.");
      setUsers([]);
      return;
    }
    if (user && !user.admin) {
      setStatus("Admin access is required for this page.");
      setUsers([]);
      return;
    }
    if (user?.admin) {
      void loadUsers();
    }
  }, [authState, user]);

  async function loadUsers() {
    try {
      setStatus("Loading users...");
      const data = await unwrap(listAdminUsers());
      setUsers(data.items);
      setStatus(`Loaded ${data.items.length} user accounts.`);
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not load users.");
    }
  }

  async function saveStatus(id: string, nextStatus: UserStatus) {
    try {
      await unwrap(updateAdminUserStatus({ path: { id }, body: { status: nextStatus } }));
      await loadUsers();
      setStatus("User status updated.");
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not update user.");
    }
  }

  return (
    <main className="admin-layout">
      <section className="hero-card compact">
        <p className="eyebrow">Admin</p>
        <h1>Manage user access.</h1>
        <p>Approve users for app features. Admin-list users are protected and cannot be edited here.</p>
        {authState === "signed-out" ? <SignInCard message="Sign in with an admin Google account." /> : null}
      </section>
      <StatusBox>{status}</StatusBox>
      <section className="users-list" aria-label="User accounts">
        {users.map((account) => <AdminUserRow key={account.id} user={account} onSave={saveStatus} />)}
      </section>
    </main>
  );
}

type PageProps = {
  user?: CurrentUserResponse;
  authState: AuthState;
  onAuthError: (error: unknown) => void;
};

function WordInfoPanel({ info, onUseWord, onUsePhrase }: { info: WordInfoResponse; onUseWord: () => void; onUsePhrase: (sentence: string) => void }) {
  return (
    <div className="word-info">
      <button type="button" className="soft-list-button" onClick={onUseWord}>
        <strong>{info.normalizedWord}</strong>
        <small>{[info.article, info.partOfSpeech, info.frequency].filter(Boolean).join(" · ")}</small>
        <small>{joinText(info.translations.en)} · {joinText(info.translations.ru)}</small>
        <small>{joinText(info.shortNote.en)} · {joinText(info.shortNote.ru)}</small>
      </button>
      {info.examples.map((example) => (
        <button key={example.sentence} type="button" className="soft-list-button" onClick={() => onUsePhrase(example.sentence)}>
          <strong>{example.sentence}</strong>
          <small>{joinText(example.translations.en)} · {joinText(example.translations.ru)}</small>
        </button>
      ))}
    </div>
  );
}

function ReviewResult({ item, result, onNext, finalItem, onAuthError }: { item: DictionaryReviewItem; result: DictionaryReviewSubmitResponse; onNext: () => void; finalItem: boolean; onAuthError: (error: unknown) => void }) {
  return (
    <div className={`result ${result.correct ? "correct" : "incorrect"}`}>
      <p><strong>{result.correct ? "Correct" : "Not this time"}</strong></p>
      <p>Answer: <strong>{result.expectedAnswer}</strong></p>
      {item.pronunciationAssetId ? <ReviewVideo assetId={item.pronunciationAssetId} onAuthError={onAuthError} /> : null}
      <small>Next due: {new Date(result.dueAt).toLocaleString()}</small>
      <button type="button" onClick={onNext}>{finalItem ? "Finish batch" : "Next word"}</button>
    </div>
  );
}

function ReviewVideo({ assetId, onAuthError }: { assetId: string; onAuthError: (error: unknown) => void }) {
  const [src, setSrc] = useState("");
  const [status, setStatus] = useState("Loading cached video...");
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    let objectUrl = "";
    unwrap(getPronunciationVideo({ path: { id: assetId }, parseAs: "blob" }))
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob);
        setSrc(objectUrl);
        setStatus("");
        window.setTimeout(() => void videoRef.current?.play(), 50);
      })
      .catch((error: unknown) => {
        onAuthError(error);
        setStatus(error instanceof Error ? error.message : "Could not load video.");
      });
    return () => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [assetId, onAuthError]);

  if (!src) {
    return <small>{status}</small>;
  }
  return <video ref={videoRef} className="review-video" src={src} controls playsInline />;
}

function AdminUserRow({ user, onSave }: { user: AdminUserResponse; onSave: (id: string, status: UserStatus) => Promise<void> }) {
  const [status, setStatus] = useState<UserStatus>(user.status);
  const [busy, setBusy] = useState(false);

  async function save() {
    setBusy(true);
    try {
      await onSave(user.id, status);
    }
    finally {
      setBusy(false);
    }
  }

  return (
    <article className={`user-row${user.adminListUser ? " protected" : ""}`}>
      <div>
        <strong>{user.email}</strong>
        <small>{user.displayName}</small>
        {user.adminListUser ? <span className="badge">Admin-list protected</span> : null}
      </div>
      <select value={status} disabled={user.adminListUser || busy} aria-label="User status" onChange={(event) => setStatus(event.target.value as UserStatus)}>
        <option value="pending">pending</option>
        <option value="active">active</option>
        <option value="deactivated">deactivated</option>
      </select>
      <button type="button" disabled={user.adminListUser || busy} onClick={save}>Save</button>
    </article>
  );
}

function AccountNotice({ user }: { user: CurrentUserResponse }) {
  return <div className="notice">{accountRestrictionMessage(user.status)} App features are disabled.</div>;
}

function SignInCard({ message }: { message: string }) {
  return (
    <div className="signin-card">
      <p>{message}</p>
      <a className="login-button wide" href={loginUrl()}><img src="/google-g.svg" alt="" aria-hidden="true" />Sign in with Google</a>
    </div>
  );
}

function StatusBox({ children }: { children: string }) {
  return <div className="status-box">{children}</div>;
}

function Prompt({ label, value }: { label: string; value: string }) {
  return (
    <div className="prompt">
      <small>{label}</small>
      <strong>{value}</strong>
    </div>
  );
}

function DoneCard() {
  return (
    <article className="study-card done-card">
      <h2>You are done, congrats!</h2>
      <p>No words are currently due. You can practice more to review the next words by due date.</p>
    </article>
  );
}

function describeSuggestion(item: WordSuggestion) {
  if (item.source === "pronunciation") {
    return `${item.phrase ?? "No phrase"}${item.status ? ` · ${item.status}` : ""}`;
  }
  return "Cached word info";
}

async function pollPronunciation(id: string, setStatus: (message: string) => void) {
  for (let attempt = 0; attempt < 180; attempt += 1) {
    const asset = await unwrap(getPronunciation({ path: { id } }));
    setStatus(`id=${asset.id}\nstatus=${asset.status}\nwaiting=${attempt * 2}s`);
    if (asset.status === "completed" || asset.status === "failed") {
      return asset;
    }
    await sleep(2000);
  }
  throw new Error("Timed out waiting for video generation.");
}

function sleep(millis: number) {
  return new Promise((resolve) => window.setTimeout(resolve, millis));
}

function joinText(values?: string[]) {
  return values?.join(" · ") ?? "";
}

function normalizeAnswer(value: string) {
  return value
    .toLowerCase()
    .replaceAll("ä", "ae")
    .replaceAll("ö", "oe")
    .replaceAll("ü", "ue")
    .replaceAll("ß", "ss")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}
