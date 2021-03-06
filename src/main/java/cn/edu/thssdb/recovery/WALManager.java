package cn.edu.thssdb.recovery;
// WAL manager 在此只需要管理b加树索引的更新
import cn.edu.thssdb.storage.BufferPool;
import cn.edu.thssdb.storage.FileHandler;
import cn.edu.thssdb.storage.Heap.HeapFile;
import cn.edu.thssdb.storage.Heap.HeapPage;
import cn.edu.thssdb.storage.Page;
import cn.edu.thssdb.utils.Global;
import jdk.nashorn.internal.runtime.regexp.joni.exception.InternalException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class WALManager {

    public static final int MAX_WAL_FILE_SIZE = 1024*1024*100;

    private File walFile;

    private HeapFile dbTable;

    private ArrayList<RecoveryInfo> recoveryList;

    private HashSet<Integer> validTxns;

    public WALManager(File walFile, HeapFile dbTable){
        this.walFile = walFile;
        this.dbTable = dbTable;
        this.recoveryList = new ArrayList<>();
    }

    public void start(int txnId){
        this.recoveryList.add(new RecoveryInfo(txnId,WALType.START_TXN));
    }

    public void abort(int txnId){
        this.recoveryList.add(new RecoveryInfo(txnId,WALType.ABORT_TXN));
    }

    public void commit(int txnId){
        this.recoveryList.add(new RecoveryInfo(txnId, WALType.COMMIT_TXN));
    }

    public void insert(int txnId, int pageNum, short pageOffset, int tid){
        this.recoveryList.add(new RecoveryInfo(txnId, WALType.INSERT_ROW, tid, pageNum, pageOffset));
    }

    public void delete(int txnId, int pageNum, short pageOffset, int tid){
        this.recoveryList.add(new RecoveryInfo(txnId, WALType.DELETE_ROW, tid, pageNum, pageOffset));
    }

    public void checkpoint(int txnId){
        this.recoveryList.add(new RecoveryInfo(txnId, WALType.CHECKPOINT));
    }


    public void persist() throws IOException{
        FileOutputStream fos = new FileOutputStream(walFile);
        DataOutputStream dos = new DataOutputStream(fos);
        for(RecoveryInfo info : recoveryList){
            info.serialize(dos);
        }
        dos.close();
        fos.close();
    }

    public void recover() throws IOException{
        FileInputStream fis = new FileInputStream(walFile);
        DataInputStream dis = new DataInputStream(fis);
        while(dis.available() != 0){
            this.recoveryList.add(RecoveryInfo.parse(dis));
        }


        analyze();
        
        redo();
        dis.close();
        fis.close();
        //由于abort掉的日志存了但是磁盘文件并不会存储,所以不需要通过redo恢复日志
        //若完成checkpoint后,将会起作用
        //undo();

    }

    private void analyze() {
        for ( RecoveryInfo info : recoveryList){
            if(info.walType == WALType.COMMIT_TXN)
                validTxns.add(info.txnId);
        }
    }

    private void undo() {
    }

    //
    private void redo() {
        ArrayList<RecoveryInfo> newRecover = new ArrayList<>();
        for(RecoveryInfo info : recoveryList) {
            if (validTxns.contains(info.txnId)) {
                switch (info.walType) {
                    case START_TXN:
                    case COMMIT_TXN:
                    case ABORT_TXN:
                    case CHECKPOINT:
                        break;
                    case DELETE_ROW:
                        dbTable.deleteIndex(info.pageNum, info.pageOffset);
                        break;
                    case INSERT_ROW:
                        dbTable.insertIndex(info.pageNum, info.pageOffset);
                        break;
                }
            }
        }

    }


    public static String getWALfileName(
            String databaseName,
            int fileId){
        return String.format(Global.LOG_FORMAT, databaseName, String.valueOf(fileId));
    }



}
