// npm run reset: seed.json -> db.json (invalidating every token) and delete
// every upload except uploads/seed/. Safe to run while the server is up.
const config = require('../src/config');
const { resetData } = require('../src/data-files');

const { removed } = resetData(config);
console.log(`Reset ${config.dbFile} from seed.json; removed ${removed.length} upload(s).`);
