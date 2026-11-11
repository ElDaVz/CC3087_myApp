import {coverImageUrl, validate, type Answer, type Field, type PublishedForm, type Value} from './form';

function node<K extends keyof HTMLElementTagNameMap>(tag: K, text?: string, className?: string): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag);
  if (text) element.textContent = text;
  if (className) element.className = className;
  return element;
}

function shell(root: HTMLElement) {
  const brand = node('header', undefined, 'site-header');
  const context = node('span', 'Formulario de solicitud', 'site-context');
  const identity = node('div', undefined, 'brand');
  const logo = node('img');
  logo.src = '/formlink-logo.svg';
  logo.alt = '';
  logo.width = 40;
  logo.height = 40;
  identity.append(logo, node('span', 'FormLink'));
  brand.append(context, identity);
  const content = node('div', undefined, 'page-content');
  root.replaceChildren(brand, content);
  return content;
}

function focusHeading(heading: HTMLElement) {
  heading.tabIndex = -1;
  heading.focus({preventScroll: true});
}

export function message(root: HTMLElement, text: string, retry?: () => void, state: 'info' | 'loading' | 'error' | 'success' = 'info') {
  const content = shell(root);
  const panel = node('section', undefined, `message-card message-${state}`);
  const icon = node('span', state === 'success' ? '✓' : state === 'error' ? '!' : '', 'message-icon');
  icon.setAttribute('aria-hidden', 'true');
  const title = node('h1', state === 'success' ? '¡Listo! Recibimos tu solicitud' : state === 'loading' ? 'Preparando tu formulario' : state === 'error' ? 'No pudimos abrir el formulario' : 'Todo empieza con un enlace');
  const copy = node('p', text, 'message-copy');
  if (state === 'loading') {
    copy.setAttribute('role', 'status');
    panel.setAttribute('aria-busy', 'true');
  }
  panel.append(icon, title, copy);
  if (retry) {
    const button = node('button', 'Reintentar', 'primary-button');
    button.type = 'button';
    button.onclick = retry;
    panel.append(button);
  }
  content.append(panel);
  if (state !== 'loading') focusHeading(title);
}

function questionTitle(tag: 'label' | 'legend', field: Field, index: number) {
  const title = node(tag, undefined, 'question-title');
  const number = node('span', String(index + 1).padStart(2, '0'), 'question-number');
  number.setAttribute('aria-hidden', 'true');
  const text = node('span', field.label, 'question-label');
  if (field.required) {
    const mark = node('span', ' *', 'required-mark');
    mark.setAttribute('aria-hidden', 'true');
    text.append(mark, node('span', ' (obligatorio)', 'sr-only'));
  } else {
    text.append(node('span', 'Opcional', 'optional-label'));
  }
  title.append(number, text);
  return title;
}

type Reader = {field: Field; read: () => Value; error: HTMLElement; controls: HTMLElement[]; focus: HTMLElement};

function question(field: Field, index: number): {card: HTMLElement; reader: Reader} {
  const isChoice = ['MULTIPLE_CHOICE', 'CHECKBOX_GROUP'].includes(field.type);
  const card = node('div', undefined, 'question-card');
  const group = isChoice ? node('fieldset', undefined, 'choice-group') : null;
  const container = group || card;
  const title = questionTitle(isChoice ? 'legend' : 'label', field, index);
  if (title instanceof HTMLLabelElement) title.htmlFor = field.id;
  container.append(title);
  const descriptionIds: string[] = [];
  if (field.description) {
    const help = node('p', field.description, 'help question-help');
    help.id = `${field.id}-help`;
    descriptionIds.push(help.id);
    container.append(help);
  }
  let read: () => Value;
  const controls: HTMLElement[] = [];
  if (isChoice || field.type === 'DROPDOWN') {
    if (field.type === 'DROPDOWN') {
      const select = node('select');
      select.id = field.id;
      select.multiple = field.config.allowMultipleAnswers;
      if (!select.multiple) {
        const empty = node('option', 'Selecciona una opción');
        empty.value = '';
        select.append(empty);
      }
      for (const option of field.config.options) {
        const item = node('option', option.label);
        item.value = option.id;
        select.append(item);
      }
      container.append(select);
      controls.push(select);
      read = () => Array.from(select.selectedOptions).map(option => option.value).filter(Boolean);
    } else {
      const options = node('div', undefined, 'choice-options');
      const inputs: HTMLInputElement[] = [];
      for (const option of field.config.options) {
        const row = node('label', undefined, 'choice-option');
        const input = node('input');
        input.type = field.config.allowMultipleAnswers ? 'checkbox' : 'radio';
        input.name = field.id;
        input.value = option.id;
        row.append(input, node('span', option.label));
        options.append(row);
        inputs.push(input);
      }
      container.append(options);
      controls.push(...inputs);
      read = () => inputs.filter(input => input.checked).map(input => input.value);
    }
  } else if (field.type === 'CHECKBOX') {
    const row = node('div', undefined, 'checkbox-answer');
    const input = node('input');
    input.type = 'checkbox';
    input.id = field.id;
    const hint = node('span', 'Marca la casilla para confirmar.', 'help');
    hint.id = `${field.id}-hint`;
    descriptionIds.push(hint.id);
    row.append(input, hint);
    container.append(row);
    controls.push(input);
    read = () => input.checked;
  } else if (field.type === 'DATE_TIME') {
    const row = node('div', undefined, 'date-time-answer');
    const date = node('input');
    date.type = 'date';
    date.id = field.id;
    controls.push(date);
    row.append(date);
    const time = node('input');
    time.type = 'time';
    time.setAttribute('aria-label', `${field.label}: hora`);
    if (field.config.includeTime) {
      row.append(time);
      controls.push(time);
    }
    container.append(row);
    read = () => ({date: date.value ? new Date(`${date.value}T00:00:00Z`) : null, minute: field.config.includeTime && time.value ? Number(time.value.slice(0, 2)) * 60 + Number(time.value.slice(3, 5)) : null});
  } else {
    const input = field.type === 'PARAGRAPH' ? node('textarea') : node('input');
    input.id = field.id;
    input.placeholder = field.config.placeholder;
    input.maxLength = 10000;
    if (input instanceof HTMLInputElement) {
      input.type = field.type === 'EMAIL' ? 'email' : 'text';
      if (field.type === 'NUMBER') input.inputMode = field.config.allowDecimals ? 'decimal' : 'numeric';
    }
    container.append(input);
    controls.push(input);
    read = () => input.value.trim();
  }
  const error = node('p', undefined, 'field-error');
  error.id = `${field.id}-error`;
  error.setAttribute('aria-live', 'polite');
  descriptionIds.push(error.id);
  container.append(error);
  for (const control of controls) {
    control.setAttribute('aria-describedby', descriptionIds.join(' '));
    if (field.required && (!isChoice || !field.config.allowMultipleAnswers)) control.setAttribute('aria-required', 'true');
  }
  if (group) {
    group.setAttribute('aria-describedby', descriptionIds.join(' '));
    card.append(group);
  }
  return {card, reader: {field, read, controls, error, focus: controls[0] || container}};
}

export function render(root: HTMLElement, form: PublishedForm, send: (answers: Answer[]) => Promise<void>) {
  const content = shell(root);
  // A missing or unavailable public image keeps the existing branded fallback.
  const banner = node('div', undefined, 'form-banner');
  banner.setAttribute('aria-hidden', 'true');
  const coverUrl = coverImageUrl(form.cover);
  if (coverUrl) {
    const image = node('img');
    image.alt = '';
    image.referrerPolicy = 'no-referrer';
    image.onload = () => banner.classList.add('has-cover');
    image.onerror = () => { image.remove(); banner.classList.remove('has-cover'); };
    image.src = coverUrl;
    banner.append(image);
  }
  const introduction = node('section', undefined, 'form-introduction');
  const count = `${form.fields.length} ${form.fields.length === 1 ? 'pregunta' : 'preguntas'}`;
  const eyebrow = node('div', undefined, 'form-eyebrow');
  eyebrow.append(node('span', 'TU SOLICITUD'), node('span', count, 'question-count'));
  const heading = node('h1', form.title);
  const subtitle = node('p', 'Completa el formulario y envía tu solicitud.', 'intro-copy');
  const requiredNote = node('p', undefined, 'required-note');
  requiredNote.append(node('span', '*', 'required-mark'), document.createTextNode(' Indica un campo obligatorio'));
  introduction.append(eyebrow, heading, subtitle);
  if (form.fields.some(field => field.required)) introduction.append(requiredNote);
  const formHeader = node('div', undefined, 'form-header');
  formHeader.append(banner, introduction);
  const element = node('form', undefined, 'request-form');
  element.noValidate = true;
  element.setAttribute('aria-label', 'Completar solicitud');
  const readers: Reader[] = [];
  form.fields.forEach((field, index) => {
    const {card, reader} = question(field, index);
    readers.push(reader);
    element.append(card);
  });
  const actions = node('div', undefined, 'form-actions');
  const status = node('p', undefined, 'submit-status');
  status.setAttribute('role', 'status');
  status.setAttribute('aria-atomic', 'true');
  const button = node('button', 'Enviar solicitud', 'primary-button');
  button.type = 'submit';
  actions.append(button, node('span', 'Revisa tus respuestas antes de enviar.', 'help'), status);
  element.append(actions);
  let busy = false;
  element.onsubmit = async event => {
    event.preventDefault();
    if (busy) return;
    let firstInvalid: HTMLElement | undefined;
    for (const reader of readers) {
      const error = validate(reader.field, reader.read());
      reader.error.textContent = error || '';
      for (const control of reader.controls) control.setAttribute('aria-invalid', String(!!error));
      reader.error.closest('.question-card')?.classList.toggle('question-invalid', !!error);
      if (error && !firstInvalid) firstInvalid = reader.focus;
    }
    if (firstInvalid) {
      status.textContent = 'Revisa los campos señalados para continuar.';
      firstInvalid.focus();
      return;
    }
    busy = true;
    button.disabled = true;
    button.textContent = 'Enviando…';
    element.setAttribute('aria-busy', 'true');
    // Keep the displayed answers identical to the snapshot being submitted.
    for (const reader of readers) for (const control of reader.controls) (control as HTMLInputElement).disabled = true;
    status.classList.remove('submit-error');
    status.textContent = 'Guardando solicitud…';
    try {
      await send(readers.map(reader => ({fieldId: reader.field.id, value: reader.read()})));
    } catch {
      status.classList.add('submit-error');
      status.textContent = 'No se confirmó el envío. Comprueba tu conexión y reintenta. Si el formulario cambió o se retiró, vuelve a abrir su enlace.';
    } finally {
      busy = false;
      button.disabled = false;
      button.textContent = 'Enviar solicitud';
      element.removeAttribute('aria-busy');
      for (const reader of readers) for (const control of reader.controls) (control as HTMLInputElement).disabled = false;
    }
  };
  const footer = node('footer', undefined, 'site-footer');
  footer.append(node('p', 'Tus respuestas se envían al propietario del formulario.'), node('p', 'Las métricas de uso no incluyen tus respuestas.'));
  content.append(formHeader, element, footer);
  focusHeading(heading);
}
