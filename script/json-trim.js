const fs = require('fs');
const ndjson = require('ndjson');

const INPUT_FILE = 'yelp_academic_dataset_review.json';
const OUTPUT_FILE = 'yelp_review_trimmed.json';
const TEST_INPUT = 'test_input.json';
const TEST_OUTPUT = 'test_output.json';

const writeToFile = (data, filename) => {
    if (!data) {
        return;
    }
    fs.appendFile(filename, data + '\n', (err) => {
        if (err) throw err;
    });
}

const trim = (filename) => fs.createReadStream(filename)
    .pipe(ndjson.parse())
    .on('data', (data) => {
        return new Promise((res, rej) => {
            if (data.text.length < 1000) {
                delete data;
            } else {
                delete data.review_id;
                delete data.business_id;
                delete data.user_id;
                delete data.useful;
                delete data.funny;
                delete data.cool;
                delete data.date;
                res(data);
            }
        }).then((data) => JSON.stringify(data))
        .then((data) => writeToFile(data, TEST_OUTPUT))
    });

trim(TEST_INPUT)
