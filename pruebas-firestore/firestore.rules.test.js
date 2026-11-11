// Pruebas de autorización de las reglas de Firestore — Healthy Chronos
// Ejecutar desde esta carpeta (pruebas-firestore). Requiere Node 18+ y Java 11+:
//   npm install
//   npm run test:rules
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { readFileSync } = require('fs');
const path = require('path');
const { doc, getDoc, setDoc, deleteDoc } = require('firebase/firestore');

let env;
const perfil = { uid: 'maria', email: 'maria@test.com', nombre: 'María Gómez', edad: 72 };
const med = { nombre: 'Losartán', dosis: '50 mg', horaToma: '08:00', categoria: 'Presión', activo: true };

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: 'healthy-chronos-test',
    firestore: { rules: readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8'), host: '127.0.0.1', port: 8080 },
  });
});

afterAll(async () => { await env.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/maria'), perfil);
    await setDoc(doc(db, 'users/maria/medicamentos/m1'), med);
  });
});

const maria = () => env.authenticatedContext('maria').firestore();
const pedro = () => env.authenticatedContext('pedro').firestore();
const anonimo = () => env.unauthenticatedContext().firestore();

test('A1 la dueña lee su perfil', () => assertSucceeds(getDoc(doc(maria(), 'users/maria'))));
test('A2 la dueña lee sus medicamentos', () => assertSucceeds(getDoc(doc(maria(), 'users/maria/medicamentos/m1'))));
test('A3 otro usuario NO lee el perfil ajeno', () => assertFails(getDoc(doc(pedro(), 'users/maria'))));
test('A4 otro usuario NO lee medicamentos ajenos', () => assertFails(getDoc(doc(pedro(), 'users/maria/medicamentos/m1'))));
test('A5 otro usuario NO crea medicamentos en una cuenta ajena', () => assertFails(setDoc(doc(pedro(), 'users/maria/medicamentos/m2'), med)));
test('A6 otro usuario NO borra medicamentos ajenos', () => assertFails(deleteDoc(doc(pedro(), 'users/maria/medicamentos/m1'))));
test('A7 sin sesión NO se lee nada', () => assertFails(getDoc(doc(anonimo(), 'users/maria'))));
test('A8 un medicamento con tipos inválidos se rechaza', () => assertFails(setDoc(doc(maria(), 'users/maria/medicamentos/m3'), { ...med, activo: 'si' })));
test('A9 la dueña SÍ crea un medicamento válido', () => assertSucceeds(setDoc(doc(maria(), 'users/maria/medicamentos/m4'), med)));
