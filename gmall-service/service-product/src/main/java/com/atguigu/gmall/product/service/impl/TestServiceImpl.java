package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import com.mysql.cj.util.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author zhc
 * @Create 2024/7/27 0:41
 */
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /*
    //1 使用本地锁
    @Override
    public synchronized void testLock() {
        // 查询Redis中的num值
        String value = this.redisTemplate.opsForValue().get("num");
        // 没有该值return
        if (StringUtils.isNullOrEmpty(value)) {
            return;
        }
        //有值就转成Int
        int num = Integer.parseInt(value);
        // 把Redis中的num值+1
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
    }
     */

    /*
    //2. v1 使用分布式锁
    // 使用setnx key value 可能会出现死锁
    @Override
    public void testLock() {
        //0.先尝试获取锁 setnx key val
        Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock", "lock");
        if (flag) {
            // 获取锁成功，执行业务代码
            // 1 查询Redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 2 没有该值return
            if (StringUtils.isNullOrEmpty(value)) {
                return;
            }
            //3 对num值进行自增加1
            int num = Integer.parseInt(value);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //4 将锁释放
            redisTemplate.delete("lock");

        }else {
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
     */


    /*
    //2. v2使用分布式锁
    //给锁一个过期时间
    //过期时间是多少？根据业务判断 avg(startTime() - endTime())
    //set key "v" ex 10 nx
    //set key value ex timeout nx; ex=setex nx=setnx;
    //能够保证上锁+给锁设置过期时间具有原子性
    //问题？？ 误删锁
    @Override
    public void testLock() {
        //0.先尝试获取锁 set key "v" ex 10 nx
        Boolean flag =
                redisTemplate.opsForValue().
                        setIfAbsent("lock", "lock",10, TimeUnit.SECONDS);

        if (flag) {
            // 获取锁成功，执行业务代码
            // 1 查询Redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 2 没有该值return
            if (StringUtils.isNullOrEmpty(value)) {
                return;
            }
            //3 对num值进行自增加1
            int num = Integer.parseInt(value);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //4 将锁释放
            redisTemplate.delete("lock");

        }else {
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
     */

    /*
    //2. v3使用分布式锁
    //误删锁。解决：使用UUID标识锁的唯一性
    //多个线程获取到资源。解决：实现锁的自动续期-->守护线程！
    //问题？？删除锁没有原子性
    @Override
    public void testLock() {
        //0.先尝试获取锁 set key "v" ex 10 nx
        String uuid = UUID.randomUUID().toString();
        Boolean flag =
                redisTemplate.opsForValue().
                        setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);

        if (flag) {
            // 获取锁成功，执行业务代码
            // 1 查询Redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 2 没有该值return
            if (StringUtils.isNullOrEmpty(value)) {
                return;
            }
            //3 对num值进行自增加1
            int num = Integer.parseInt(value);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //使用守护线程 自动续期
            Thread thread = new Thread(() -> {
                this.redisTemplate.expire("lock", 10, TimeUnit.SECONDS);
            });
            thread.setDaemon(true);
            thread.start();

            //4 如果uuid相等，表示是自己的锁，将锁释放
            if(uuid.equals(this.redisTemplate.opsForValue().get("lock"))){
                redisTemplate.delete("lock");
            }
        } else {
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
     */

    //2. v4使用分布式锁
    //删除锁没有原子性：使用lua脚本实现判断uuid一致和删除锁原子性
    //问题？？redis集群情况下是锁不住资源的
    // 解决方案：redisson-reLock!
    @Override
    public void testLock() {
        //0.先尝试获取锁 set key "v" ex 10 nx
        String uuid = UUID.randomUUID().toString();
        Boolean flag =
                redisTemplate.opsForValue().
                        setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);

        if (flag) {
            // 获取锁成功，执行业务代码
            // 1 查询Redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 2 没有该值return
            if (StringUtils.isNullOrEmpty(value)) {
                return;
            }
            //3 对num值进行自增加1
            int num = Integer.parseInt(value);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //使用守护线程 自动续期
            Thread thread = new Thread(() -> {
                this.redisTemplate.expire("lock", 10, TimeUnit.SECONDS);
            });
            thread.setDaemon(true);
            thread.start();

            //使用lua脚本实现(判断uuid一致和删除锁)具有原子性
            //4 如果uuid相等，表示是自己的锁，将锁释放
            String scriptText = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                    "then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            //设置Lua脚本
            DefaultRedisScript redisScript = new DefaultRedisScript();
            redisScript.setScriptText(scriptText);
            redisScript.setResultType(Long.class);
            // lua脚本,key,value
            this.redisTemplate.
                    execute(redisScript, Arrays.asList("lock"),uuid);
        } else {
            //自旋
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void testRedissonLock() {
        //0.创建锁对象
        RLock lock = redissonClient.getLock("lock1");

        try {
            //0.1 尝试加锁
            //0.1.1 lock() 阻塞等待一直到获取锁,默认锁有效期30s
            //lock.lock();
            //0.1.2 lock(数字，时间单位) 指定获取锁成功有效期，直到获取锁成功。到期自动释放
            //lock.lock(10, TimeUnit.SECONDS);
            //0.1.3 tryLock(等待获取锁时间，锁的有效期，时间单位) 指定时间内如果获取锁成功，返回true 执行后续业务，如果超过等待时间，返回false
            boolean flag = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (flag) {
                System.out.println(Thread.currentThread().getName() + "线程获取锁成功");
                //获取成功 执行业务
                //1.先从redis中通过key num获取值  key提前手动设置 num 初始值：0
                String value = redisTemplate.opsForValue().get("num");
                //2.如果值为空则非法直接返回即可
                if (StringUtils.isNullOrEmpty(value)) {
                    return;
                }
                //3.对num值进行自增加一
                int num = Integer.parseInt(value);
                redisTemplate.opsForValue().set("num", String.valueOf(++num));

                //测试可重入锁
                this.check();

                //4.将锁释放
                lock.unlock();
            } else {
                //本地获取锁失败
                Thread.sleep(100);
                this.testLock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            //如果执行异常，释放锁
            lock.unlock();
        }
    }
    /**
     * 测试可重入
     */
    private void check() {
        //尝试获取锁
        RLock lock = redissonClient.getLock("lock1");
        lock.lock();

        //执行业务
        System.out.println(Thread.currentThread().getName() + "可重入获取成功");

        //释放锁
        lock.unlock();
    }

    /**
     * 从Redis中读数据
     *
     * @return
     */
    @Override
    public String read() {
        //1.创建读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        //2.获取读锁
        RLock lock = readWriteLock.readLock();
        //加锁 给锁的有效期设置为10s
        lock.lock(10, TimeUnit.SECONDS);

        //3.执行从redis获取数据业务
        String msg = redisTemplate.opsForValue().get("msg");

        //4.释放读锁
//        lock.unlock();
        return msg;
    }

    /**
     * 将数据写入redis
     *
     * @return
     */
    @Override
    public void write() {
        //1.创建读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        //2.获取写锁
        RLock lock = readWriteLock.writeLock();
        //加锁 给锁的有效期设置为10s
        lock.lock(10, TimeUnit.SECONDS);

        //3.业务将数据写入redis
        redisTemplate.opsForValue().set("msg", "msgData");
        //4.释放写锁
//        lock.unlock();
    }
}
