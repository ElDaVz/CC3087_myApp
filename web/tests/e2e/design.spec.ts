import {test, expect, type Page} from '@playwright/test';
import type {Field, PublishedForm} from '../../src/form';

// Isolated visual fixtures: no demo route, submissions or Analytics in the shipped app.
function field(id: string, type: string, label: string, config: Partial<Field['config']> = {}): Field {
  return {id, type, label, description: '', required: false, optionIds: ['a', 'b'], config: {
    placeholder: '', allowMultipleAnswers: false, includeTime: false, disallowPastDates: false,
    allowDecimals: true, minimum: '', maximum: '', unit: '', options: [{id: 'a', label: 'Primera opción'}, {id: 'b', label: 'Segunda opción'}], ...config,
  }};
}
const form: PublishedForm = {
  ownerId: 'fixture-owner', title: 'Hagamos realidad tu próximo pedido', version: 1, active: true, templateType: 'ORDER_FORM', fields: [
    {...field('request', 'SHORT_TEXT', '¿Qué tienes en mente?', {placeholder: 'Escribe el nombre de tu pedido'}), required: true, description: 'Cuéntanos qué necesitas para empezar.'},
    field('details', 'PARAGRAPH', 'Dale tu toque personal', {placeholder: 'Colores, detalles o instrucciones especiales…'}),
    field('quantity', 'NUMBER', '¿Cuántas unidades necesitas?', {minimum: '1'}),
    field('email', 'EMAIL', 'Correo de contacto', {placeholder: 'nombre@ejemplo.com'}),
    field('delivery', 'MULTIPLE_CHOICE', '¿Cómo quieres recibirlo?'),
    field('extras', 'CHECKBOX_GROUP', 'Agrega los detalles que quieras', {allowMultipleAnswers: true}),
    field('size', 'DROPDOWN', 'Elige una presentación'),
    field('date', 'DATE_TIME', '¿Para cuándo lo necesitas?', {includeTime: true}),
    field('confirm', 'CHECKBOX', 'He revisado los detalles de mi pedido'),
  ],
};
async function fixture(page: Page, value = form, state?: string) {
  await page.route('**/src/main.ts', route => route.fulfill({contentType: 'application/javascript', body: `
    import '/src/style.css';
    import {render, message} from '/src/view.ts';
    const root = document.querySelector('#app');
    window.sent = [];
    if (${JSON.stringify(state || '')}) {
      const state = ${JSON.stringify(state || 'info')};
      const copy = {loading: 'Cargando formulario…', error: 'Este formulario no existe o ya no recibe solicitudes.', success: 'Tu solicitud se guardó correctamente. Gracias.', info: 'Abre el enlace de un formulario para completar una solicitud.'};
      message(root, copy[state], state === 'error' ? () => message(root, 'Reintento listo') : undefined, state);
    } else {
      render(root, ${JSON.stringify(value)}, async answers => {
        window.sent.push(answers);
        await new Promise(resolve => setTimeout(resolve, 250));
        if (window.sent.length === 1) throw Error('Offline fixture');
        message(root, 'Tu solicitud se guardó correctamente. Gracias.', undefined, 'success');
      });
    }
  `}));
  await page.goto('/f/design-fixture');
}

for (const width of [320, 390, 768, 1440]) {
  test(`form layout fits ${width}px and places the app logo on the right`, async ({page}) => {
    await page.setViewportSize({width, height: 900});
    await fixture(page);
    await expect(page.getByRole('heading', {name: form.title})).toBeVisible();
    const logo = page.locator('.brand img');
    await expect(logo).toBeVisible();
    expect(await logo.evaluate(image => (image as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
    const header = await page.locator('.site-header').boundingBox();
    const brand = await page.locator('.brand').boundingBox();
    expect(brand!.x + brand!.width).toBeCloseTo(header!.x + header!.width, 0);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    for (const control of await page.locator('input, textarea, select, button').all()) {
      const bounds = await control.boundingBox();
      expect(bounds!.x).toBeGreaterThanOrEqual(0);
      expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(width);
    }
    await page.screenshot({path: `test-results/web-design-${width}.png`, fullPage: true});
  });
}

test('all question controls remain usable with validation, failure, retry and confirmed success', async ({page}) => {
  await fixture(page);
  await page.keyboard.press('Tab');
  await expect(page.getByLabel('¿Qué tienes en mente?')).toBeFocused();
  await page.getByRole('button', {name: 'Enviar solicitud'}).click();
  await expect(page.getByLabel('¿Qué tienes en mente?')).toBeFocused();
  await expect(page.getByText('Completa este campo.')).toBeVisible();
  await page.getByLabel('¿Qué tienes en mente?').fill('Pedido sintético');
  await page.getByLabel('Dale tu toque personal').fill('Primera línea\nSegunda línea');
  await page.getByLabel('¿Cuántas unidades necesitas?').fill('3');
  await page.getByLabel('Correo de contacto').fill('fixture@example.com');
  await page.getByRole('group', {name: '¿Cómo quieres recibirlo?'}).getByLabel('Primera opción').check();
  await page.getByRole('group', {name: 'Agrega los detalles que quieras'}).getByLabel('Segunda opción').check();
  await page.getByLabel('Elige una presentación').selectOption('a');
  await page.getByLabel('¿Para cuándo lo necesitas?', {exact: false}).first().fill('2027-01-01');
  await page.getByLabel('¿Para cuándo lo necesitas?: hora', {exact: true}).fill('14:30');
  await page.getByLabel('He revisado los detalles de mi pedido').check();
  await page.getByRole('button', {name: 'Enviar solicitud'}).click();
  await expect(page.getByRole('button', {name: 'Enviando…'})).toBeDisabled();
  await expect(page.getByLabel('¿Qué tienes en mente?')).toBeDisabled();
  await expect(page.getByRole('status')).toContainText('No se confirmó');
  await expect(page.getByLabel('¿Qué tienes en mente?')).toHaveValue('Pedido sintético');
  await expect(page.getByLabel('Dale tu toque personal')).toHaveValue('Primera línea\nSegunda línea');
  await page.getByRole('button', {name: 'Enviar solicitud'}).click();
  await expect(page.getByRole('heading', {name: '¡Listo! Recibimos tu solicitud'})).toBeFocused();
  await expect(page.getByText('Tu solicitud se guardó correctamente. Gracias.')).toBeVisible();
  const sent = await page.evaluate(() => (window as unknown as {sent: unknown[][]}).sent);
  expect(sent).toHaveLength(2);
  expect(sent[1]).toEqual(sent[0]);
  expect(sent[1]).toContainEqual({fieldId: 'date', value: {date: new Date('2027-01-01T00:00:00.000Z'), minute: 870}});
});

test('long content and enlarged text fit a narrow screen', async ({page}) => {
  await page.setViewportSize({width: 320, height: 800});
  const long = structuredClone(form);
  long.title = 'FormularioConUnTítuloMuyLargoSinEspacios'.repeat(4);
  long.fields[0].label = 'PreguntaSinEspacios'.repeat(8);
  long.fields[4].config.options[0].label = 'OpciónSinEspacios'.repeat(8);
  await fixture(page, long);
  await page.addStyleTag({content: ':root {font-size: 200%}'});
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

for (const state of ['loading', 'error', 'success', 'info']) {
  test(`${state} screen uses the shared brand and accessible feedback`, async ({page}) => {
    await fixture(page, form, state);
    await expect(page.locator('.brand')).toContainText('FormLink');
    await expect(page.getByRole('heading')).toBeVisible();
    if (state === 'loading') await expect(page.getByRole('status')).toHaveText('Cargando formulario…');
    else await expect(page.getByRole('heading')).toBeFocused();
    if (state === 'error') {
      await page.getByRole('button', {name: 'Reintentar'}).click();
      await expect(page.getByText('Reintento listo')).toBeVisible();
    }
    await page.screenshot({path: `test-results/web-${state}.png`, fullPage: true});
  });
}

test('Cloudinary banner loads responsively and falls back when the image fails',async({page})=>{
 const withCover=structuredClone(form);
 withCover.cover={url:'https://res.cloudinary.com/uxrkjzti/image/upload/v123/fixture.jpg',publicId:'fixture',hash:'a'.repeat(64)};
 await page.route(withCover.cover.url,route=>route.fulfill({contentType:'image/svg+xml',body:'<svg xmlns="http://www.w3.org/2000/svg" width="800" height="200"><rect width="800" height="200" fill="#6650a4"/></svg>'}));
 await fixture(page,withCover);
 await expect(page.locator('.form-banner')).toHaveClass(/has-cover/);
 await expect(page.getByRole('heading',{name:form.title})).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.unroute(withCover.cover.url);
 await page.route(withCover.cover.url,route=>route.abort());
 await page.reload();
 await expect(page.locator('.form-banner img')).toHaveCount(0);
 await expect(page.getByRole('button',{name:'Enviar solicitud'})).toBeVisible();
});
