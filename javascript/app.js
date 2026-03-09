import express from 'express';
import { createClient } from 'redis';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());
app.use(express.text());

// Create Redis client
const client = createClient({
  socket: {
    host: process.env.REDIS_HOST || 'localhost',
    port: process.env.REDIS_PORT || 6379
  }
});

client.on('error', (err) => console.log('Redis Client Error', err));

// Connect to Redis
await client.connect();

app.get('/', (req, res) => {
  res.json({ message: 'Redis JavaScript Workshop - Ready!' });
});

app.get('/ping', async (req, res) => {
  try {
    const pong = await client.ping();
    res.json({ status: 'connected', ping: pong });
  } catch (error) {
    res.json({ status: 'error', message: error.message });
  }
});

app.post('/set/:key', async (req, res) => {
  try {
    const { key } = req.params;
    const value = req.body;
    await client.set(key, value);
    res.json({ status: 'success', key, value });
  } catch (error) {
    res.json({ status: 'error', message: error.message });
  }
});

app.get('/get/:key', async (req, res) => {
  try {
    const { key } = req.params;
    const value = await client.get(key);
    res.json({ status: 'success', key, value });
  } catch (error) {
    res.json({ status: 'error', message: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

