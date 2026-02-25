export const STORAGE_KEY = 'distributedLocksProgress';

// Lock types available in the workshop
export const LOCK_TYPES = [
  {
    id: 'reentrant',
    name: 'Reentrant Lock',
    description: 'Same thread can acquire the lock multiple times without deadlocking.',
    useCase: 'Nested method calls that all need exclusive access to a resource.',
    difficulty: 'Beginner',
    available: true
  },
  {
    id: 'fair',
    name: 'Fair Lock',
    description: 'First-come, first-served ordering. No thread waits forever.',
    useCase: 'Ticket queues, resource allocation where order matters.',
    difficulty: 'Beginner',
    available: false
  },
  {
    id: 'readwrite',
    name: 'Read/Write Lock',
    description: 'Multiple readers OR one writer. Optimized for read-heavy workloads.',
    useCase: 'Config caches, shared data with rare updates.',
    difficulty: 'Intermediate',
    available: false
  },
  {
    id: 'semaphore',
    name: 'Semaphore',
    description: 'Allow N concurrent holders instead of just one.',
    useCase: 'Connection pools, rate limiting, bounded concurrency.',
    difficulty: 'Intermediate',
    available: false
  },
  {
    id: 'countdownlatch',
    name: 'CountDownLatch',
    description: 'Wait for N events before proceeding.',
    useCase: 'Startup coordination, batch processing barriers.',
    difficulty: 'Advanced',
    available: false
  }
];

// Default progress structure
const defaultProgress = () => ({
  currentStage: 1,  // 1 = problem, 2 = overview
  problemSeen: false,
  overviewComplete: false,
  lockTypes: {
    reentrant: { started: false, implemented: false, testsComplete: false },
    fair: { started: false, implemented: false, testsComplete: false },
    readwrite: { started: false, implemented: false, testsComplete: false },
    semaphore: { started: false, implemented: false, testsComplete: false },
    countdownlatch: { started: false, implemented: false, testsComplete: false }
  }
});

export const loadProgress = () => {
  const saved = window.localStorage.getItem(STORAGE_KEY);
  if (!saved) {
    return defaultProgress();
  }
  try {
    const parsed = JSON.parse(saved);
    // Merge with defaults to handle new fields
    return { ...defaultProgress(), ...parsed };
  } catch (error) {
    return defaultProgress();
  }
};

export const saveProgress = (progress) => {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
};

export const clearProgress = () => {
  window.localStorage.removeItem(STORAGE_KEY);
};

export const getLockTypeProgress = (lockTypeId) => {
  const progress = loadProgress();
  return progress.lockTypes[lockTypeId] || { started: false, implemented: false, testsComplete: false };
};

export const updateLockTypeProgress = (lockTypeId, updates) => {
  const progress = loadProgress();
  progress.lockTypes[lockTypeId] = { ...progress.lockTypes[lockTypeId], ...updates };
  saveProgress(progress);
};

export const isLockTypeComplete = (lockTypeId) => {
  const lockProgress = getLockTypeProgress(lockTypeId);
  return lockProgress.testsComplete;
};

export const getCompletedLockTypesCount = () => {
  const progress = loadProgress();
  return Object.values(progress.lockTypes).filter(lt => lt.testsComplete).length;
};

export const fetchLockStatus = async () => {
  try {
    const [propertiesResponse, lockResponse] = await Promise.all([
      fetch('/api/editor/file/application.properties'),
      fetch('/api/editor/file/LockManager.java')
    ]);
    const propertiesData = await propertiesResponse.json();
    const lockData = await lockResponse.json();
    const propertiesContent = propertiesData?.content || '';
    const lockContent = lockData?.content || '';
    // Check if lock mode is set to redisson (flexible matching)
    const configured = propertiesContent.includes('workshop.lock.mode=redisson');
    // Check if lock implementation exists
    const implemented = lockContent.includes('tryLock(') && lockContent.includes('unlock()');
    return {
      configured,
      implemented,
      enabled: configured && implemented
    };
  } catch (error) {
    return {
      configured: false,
      implemented: false,
      enabled: false
    };
  }
};
