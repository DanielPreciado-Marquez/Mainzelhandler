'use strict';

QUnit.config.autostart = false;

var pseudonymizationService;

window.onload = function () {
    pseudonymizationService = new PseudonymizationService(contextPath);
    QUnit.start();
}

QUnit.module('batch-test', () => {
    /**
     * 1: 2457, 1129, 2146
     * 100: -, 53584
     */

    // TODO: Tests failing because patients have the same pseudonym; i = 11/ 111, 22/ 222, ...

    let patients = new Map();
    const amount = 112;

    QUnit.test('batch pseudonymization', async assert => {
        /**
         * 1: 2235, 920, 1928
         * 100: 53609, 52821
         */

        assert.expect(amount * 3);

        // send
        for (let i = 0; i < amount; ++i) {
            const patient = pseudonymizationService.createPatient("BatchTestPatient", i.toString(), "01-01-2000", { height: i });
            patient.sureness = true;
            patients.set(i, patient);
        }

        await pseudonymizationService.sendPatients(patients);

        for (const [key, patient] of patients.entries()) {
            assert.strictEqual(patient.status, PatientStatus.SAVED, "Compare status of patient " + key);
        }

        // request
        patients = new Map();

        for (let i = 0; i < amount; ++i) {
            const patient = pseudonymizationService.createPatient("BatchTestPatient", i.toString(), "01-01-2000");
            patients.set(i, patient);
        }

        await pseudonymizationService.requestPatients(patients);

        for (const [key, patient] of patients.entries()) {
            assert.strictEqual(patient.status, PatientStatus.FOUND, "Compare status of patient " + key);
            assert.deepEqual(patient.mdat, { height: key }, "Compare MDAT of patient " + key);
        }
    });

    QUnit.test('batch depseudonymization', async assert => {
        /**
         * 1: 210, 198, 207
         * 100: 3763, 753
         */

        assert.expect(amount * 2 + 2);

        const pseudonyms = [];

        for (const patient of patients.values())
            pseudonyms.push(patient.pseudonym);

        const { depseudonymized, invalid } = await pseudonymizationService.depseudonymize(pseudonyms);

        assert.strictEqual(invalid.length, 0, "Check amount of invalid pseudonyms");
        assert.strictEqual(depseudonymized.size, amount, "Check amount of depseudonymized");

        for (const [key, patient] of patients.entries()) {
            const pseudonym = patient.pseudonym;

            assert.true(depseudonymized.has(pseudonym), "Check pseudonym of patient " + key);
            const idat = depseudonymized.get(pseudonym);
            assert.deepEqual(idat, patient.idat, "Check IDAT of patient " + key);
        }
    });
});
