const restify = require('restify');
const crypto = require('crypto');
const fetch = require('node-fetch');
const jwt = require('jsonwebtoken');

const keyPair = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
const server = restify.createServer();
server.use(restify.plugins.bodyParser());

async function getIdentity(userId) {
    const response = await fetch('http://localhost:9000/user?' + new URLSearchParams({ userId }));
    if (response.status < 200 || response.status >= 300) {
        throw 'HTTP response status ' + response.status;
    }

    return await response.json();
}

server.post('/token', async (req, res) => {
    const request = req.body;

    const identity = await getIdentity(request.userId);

    const token = jwt.sign({
        sub: request.userId,
        given_name: identity.firstName,
        family_name: identity.lastName,
        iat: Date.now()
    }, keyPair.privateKey, { algorithm: 'RS256' });

    res.json({ token });
});

const port = 8003;
server.listen(port, () => console.log(`Started at port ${port}`));