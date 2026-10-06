'use strict';

const $ = id => document.getElementById(id);

function show(el, html, isError) {
  el.className = 'result' + (isError ? ' error' : '');
  el.innerHTML = html;
}

async function run(resultEl, request, render) {
  try {
    show(resultEl, render(await request()));
  } catch (err) {
    show(resultEl, err.messages.map(esc).join('<br>'), true);
  }
}

$('btnMinWeapon').onclick = () => run($('resMinWeapon'), () => api('GET', '/api/special/min-weapon'), marineCardHtml);
$('btnMaxId').onclick = () => run($('resMaxId'), () => api('GET', '/api/special/max-id'), marineCardHtml);
$('btnCategories').onclick = () => run($('resCategories'), () => api('GET', '/api/special/categories'),
  list => list.length === 0 ? 'Нет объектов' : '<code>[' + list.map(c => '"' + c + '"').join(', ') + ']</code>');

/* ---------- 4. добавить десантника в орден ---------- */

$('addFields').innerHTML = marineFieldsHtml();
$('addFields').querySelector('[name=chapterId]').closest('label').firstChild.textContent = 'Целевой орден';

$('addForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const marine = readMarine(ev.target);
  const errs = validateMarine(marine);
  showErrors($('addErrors'), errs);
  if (errs.length) return;
  await run($('resAdd'), () => api('POST', `/api/special/chapters/${marine.chapterId}/marines`, marine),
    m => 'Добавлен.' + marineCardHtml(m));
  await refreshChapters();
};

/* ---------- 5. распустить орден ---------- */

$('dissolveForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const e = ev.target.elements;
  const id = e.chapter.value;
  if (!id) { show($('resDissolve'), 'Выберите орден', true); return; }
  if (e.target.value === id) { show($('resDissolve'), 'Нельзя перевести десантников в распускаемый орден', true); return; }
  if (!confirm('Распустить орден #' + id + '?')) return;
  const url = '/api/chapters/' + id + (e.target.value ? '?reassignTo=' + encodeURIComponent(e.target.value) : '');
  await run($('resDissolve'), () => api('DELETE', url), () => 'Орден #' + id + ' распущен.');
  await refreshChapters();
};

/* ---------- списки орденов ---------- */

async function refreshChapters() {
  const addSelect = $('addForm').elements.chapterId;
  const prevAdd = addSelect.value;
  await fillChapterSelect(addSelect, prevAdd ? Number(prevAdd) : null);

  const dForm = $('dissolveForm').elements;
  const list = await api('GET', '/api/chapters');
  const options = list.map(c => `<option value="${c.id}">${c.id} — ${esc(c.name)}</option>`).join('');
  const prevChapter = dForm.chapter.value;
  const prevTarget = dForm.target.value;
  dForm.chapter.innerHTML = options;
  dForm.target.innerHTML = '<option value="">— не требуется (в ордене нет десантников) —</option>' + options;
  if (list.some(c => String(c.id) === prevChapter)) dForm.chapter.value = prevChapter;
  if (list.some(c => String(c.id) === prevTarget)) dForm.target.value = prevTarget;
}

renderMenu('special.html');
refreshChapters().catch(err => alert(err.message));
subscribeChanges(() => refreshChapters().catch(() => {}));
