'use strict';
const $ = (id) => document.getElementById(id);
const state = { query: '', genre: '', sort: 'rating', minimum: 0, offset: 0, limit: 12, total: 0 };
let requestId = 0;
let detailRequest = 0;
let genres = [];
let totalMovies = 0;
const format = new Intl.NumberFormat('en-GB');
function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}
async function api(path) {
  const response = await fetch(path);
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'The request could not be completed.');
  return data;
}
function year(movie) { return movie.releaseDate ? movie.releaseDate.slice(0, 4) : 'Date unknown'; }
function score(movie) {
  const node = element('span', 'score', '★');
  node.append(element('b', '', movie.ratingCount ? movie.rating.toFixed(1) : '—'), element('small', '', '/ 5'));
  return node;
}
function movieCard(movie) {
  const button = element('button', 'movie-card');
  button.setAttribute('aria-label', `View ${movie.title}`);
  const poster = element('div', `poster tone-${movie.id % 6}`);
  poster.setAttribute('aria-hidden', 'true');
  const initials = movie.title.split(/\s+/).slice(0, 2).map(word => word[0]).join('');
  poster.append(element('span', 'film-code', `CI / ${String(movie.id).padStart(3, '0')}`), element('span', 'monogram', initials), element('span', 'poster-genre', movie.genres[0] || 'CINEMA'));
  const info = element('div', 'movie-info');
  info.append(element('h3', 'movie-title', movie.title), element('div', 'movie-subtitle', `${year(movie)} · ${movie.runtime || '—'} min · ${movie.genres[0] || 'Film'}`));
  const bottom = element('div', 'movie-bottom');
  bottom.append(score(movie), element('span', '', `${format.format(movie.ratingCount)} ratings`), element('span', 'movie-arrow', '↗'));
  info.append(bottom);
  button.append(poster, info);
  button.addEventListener('click', () => showMovie(movie.id));
  return button;
}
function renderGenres() {
  $('genre-list').replaceChildren();
  for (const genre of [{ name: '', count: totalMovies }, ...genres]) {
    const selected = state.genre === genre.name;
    const button = element('button', `genre-button${selected ? ' selected' : ''}`, genre.name || 'All films');
    button.setAttribute('aria-pressed', String(selected));
    button.append(element('span', '', format.format(genre.count)));
    button.addEventListener('click', () => { state.genre = genre.name; state.offset = 0; renderGenres(); loadMovies(); });
    $('genre-list').append(button);
  }
}
async function loadMovies() {
  const current = ++requestId;
  $('error').hidden = true;
  $('movie-grid').setAttribute('aria-busy', 'true');
  $('result-count').textContent = 'Searching the index…';
  try {
    const query = new URLSearchParams({ q: state.query, genre: state.genre, sort: state.sort, minRatings: state.minimum, offset: state.offset, limit: state.limit });
    const result = await api(`/api/movies?${query}`);
    if (current !== requestId) return;
    state.total = result.total;
    $('movie-grid').replaceChildren(...result.items.map(movieCard));
    $('empty').hidden = result.total !== 0;
    $('result-count').textContent = `${format.format(result.total)} ${result.total === 1 ? 'film' : 'films'}${state.genre ? ` · ${state.genre}` : ' in the collection'}`;
    $('query-time').textContent = `${result.queryMs.toFixed(2)} ms / query`;
    $('previous').disabled = state.offset === 0;
    $('next').disabled = state.offset + state.limit >= result.total;
    $('page-label').textContent = result.total ? `${state.offset + 1}–${Math.min(state.offset + state.limit, result.total)} of ${format.format(result.total)}` : '0 results';
  } catch (error) {
    if (current !== requestId) return;
    $('error').textContent = `${error.message} Check that the local engine is running, then change a filter to retry.`;
    $('error').hidden = false;
    $('result-count').textContent = 'Unable to load films';
    $('movie-grid').replaceChildren();
    $('previous').disabled = $('next').disabled = true;
    $('page-label').textContent = '';
  } finally { if (current === requestId) $('movie-grid').setAttribute('aria-busy', 'false'); }
}
function openDetail() {
  $('detail-content').replaceChildren(element('div', 'detail-body', 'Loading…'));
  if (!$('detail-dialog').open) $('detail-dialog').showModal();
}
function recommendation(movie, explanation) {
  const button = element('button', 'recommendation', `${movie.title} ↗`);
  button.append(element('small', '', explanation));
  button.addEventListener('click', () => showMovie(movie.id));
  return button;
}
async function showMovie(id) {
  const current = ++detailRequest;
  openDetail();
  try {
    const movie = await api(`/api/movies/${id}`);
    if (current !== detailRequest) return;
    const hero = element('div', 'detail-hero');
    hero.append(element('div', 'eyebrow', `FILM / ${String(id).padStart(3, '0')}`));
    const title = element('h2', '', movie.title); title.id = 'detail-title';
    hero.append(title, element('div', 'movie-subtitle', `${year(movie)} · ${movie.runtime || '—'} minutes`), element('p', '', movie.overview || 'No overview available.'));
    const tags = element('div', 'detail-tags');
    for (const genre of movie.genres) tags.append(element('span', 'tag', genre));
    hero.append(tags);
    const body = element('div', 'detail-body');
    const columns = element('div', 'detail-columns');
    const cast = element('section'); cast.append(element('h3', '', 'The people behind the film'));
    const list = element('div', 'cast-list');
    for (const person of movie.cast) {
      const button = element('button', 'cast-button', `${person.name} ↗`);
      button.append(element('small', '', person.character));
      button.addEventListener('click', () => showCast(person.id));
      list.append(button);
    }
    if (!movie.cast.length) list.append(element('p', 'muted small', 'No cast information available.'));
    for (const person of movie.crew) list.append(element('div', 'muted small', `${person.name} · ${person.job || 'Crew'}`));
    cast.append(list);
    const ratings = element('section'); ratings.append(element('h3', '', 'Audience signal'), score(movie), element('p', 'muted small', `${format.format(movie.ratingCount)} ratings · 0–5 scale`));
    const histogram = element('div', 'histogram');
    histogram.setAttribute('role', 'img'); histogram.setAttribute('aria-label', movie.ratingHistogram.map((count, i) => `${i / 2} stars: ${count}`).join('; '));
    const max = Math.max(1, ...movie.ratingHistogram);
    movie.ratingHistogram.forEach((count, i) => {
      const column = element('div', 'histogram-column');
      // A local SVG has no inline styles and remains compatible with the strict CSP.
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('viewBox', '0 0 20 80'); svg.setAttribute('width', '100%'); svg.setAttribute('height', '80');
      const bar = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
      const height = Math.max(1, count / max * 78);
      for (const [key, value] of Object.entries({ x: 1, y: 80 - height, width: 18, height, fill: '#9daf7c', rx: 2 })) bar.setAttribute(key, value);
      svg.append(bar); column.append(svg, element('span', 'histogram-label', String(i / 2))); histogram.append(column);
    });
    ratings.append(histogram); columns.append(cast, ratings); body.append(columns);
    const similar = element('section', 'detail-section'); similar.append(element('h3', '', 'Connected by more than a title'));
    for (const item of movie.recommendations) similar.append(recommendation(item, item.reasons.join(' · ')));
    if (!movie.recommendations.length) similar.append(element('p', 'muted small', 'No shared genres or cast found in this catalogue.'));
    body.append(similar); $('detail-content').replaceChildren(hero, body); $('detail-dialog').scrollTop = 0;
  } catch (error) { if (current === detailRequest) $('detail-content').replaceChildren(element('div', 'detail-body', error.message)); }
}
async function showCast(id) {
  const current = ++detailRequest; openDetail();
  try {
    const person = await api(`/api/cast/${id}`);
    if (current !== detailRequest) return;
    const body = element('div', 'detail-body');
    body.append(element('div', 'eyebrow', 'CAST / FILMOGRAPHY'));
    const title = element('h2', '', person.name); title.id = 'detail-title'; body.append(title);
    for (const movie of person.films) body.append(recommendation(movie, `${year(movie)} · ${movie.genres.join(' / ')}`));
    $('detail-content').replaceChildren(body);
  } catch (error) { if (current === detailRequest) $('detail-content').replaceChildren(element('div', 'detail-body', error.message)); }
}
$('about-button').addEventListener('click', () => $('about-dialog').showModal());
$('close-about').addEventListener('click', () => $('about-dialog').close());
$('close-detail').addEventListener('click', () => $('detail-dialog').close());
$('detail-dialog').addEventListener('close', () => { detailRequest++; });
let searchTimer;
$('search').addEventListener('input', event => {
  clearTimeout(searchTimer); requestId++; state.query = event.target.value; state.offset = 0;
  searchTimer = setTimeout(loadMovies, 180);
});
$('sort').addEventListener('change', event => { state.sort = event.target.value; state.offset = 0; loadMovies(); });
$('minimum').addEventListener('change', event => { state.minimum = Number(event.target.value); state.offset = 0; loadMovies(); });
$('reset-button').addEventListener('click', () => {
  clearTimeout(searchTimer); Object.assign(state, { query: '', genre: '', minimum: 0, offset: 0, sort: 'rating' });
  $('search').value = ''; $('minimum').value = '0'; $('sort').value = 'rating'; renderGenres(); loadMovies();
});
$('previous').addEventListener('click', () => { state.offset = Math.max(0, state.offset - state.limit); loadMovies(); });
$('next').addEventListener('click', () => { state.offset += state.limit; loadMovies(); });
document.addEventListener('keydown', event => {
  if (event.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName) && !$('detail-dialog').open && !$('about-dialog').open) {
    event.preventDefault(); $('search').focus();
  }
});
async function initialize() {
  try {
    const stats = await api('/api/stats');
    totalMovies = stats.movies; genres = stats.genres;
    $('movie-count').textContent = format.format(stats.movies);
    $('rating-count').textContent = format.format(stats.ratings);
    $('cast-count').textContent = format.format(stats.cast);
    $('average-rating').textContent = stats.ratings ? `${stats.averageRating.toFixed(2)} / 5` : '—';
    $('source-name').textContent = stats.source;
    $('source-description').textContent = stats.source === 'Fictional demo' ? 'Original sample films and synthetic ratings.' : 'Your catalogue, loaded locally.';
    renderGenres(); await loadMovies();
  } catch (error) { $('error').textContent = `${error.message} Reload the page to retry.`; $('error').hidden = false; $('result-count').textContent = 'Engine unavailable'; }
}
initialize();
