// MongoDB init script — auth servisi başlarken çalışır.
// admin kullanıcısını yoksa oluşturur (idempotent).
db = db.getSiblingDB('authdb');

if (db.users.countDocuments({ username: 'admin' }) === 0) {
  db.users.insertOne({
    username: 'admin',
    password: '$2a$10$ij0iEF6Kquf1DGfLACSbVO0Is1NuyefEjBTe7Wphz1ZMUvnNPVAMu',
    email: 'admin@system.com',
    _class: 'com.yazlab.auth.model.User'
  });
}
