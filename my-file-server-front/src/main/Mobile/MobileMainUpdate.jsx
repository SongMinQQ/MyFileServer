import { useCallback, useEffect, useState } from 'react';
import s from './MobileMainUpdate.module.css';
import Folder from './Component/Folder';
import File from './Component/File';
import FileDetailMenu from './Component/FileDetailMenu';
import Sidebar from './Component/Sidebar';
import UploadButton from './Component/UploadButton';
import api from '../../common/api';

const MobileMainUpdate = ({ user }) => {
    const [history, setHistory] = useState([]);

    const [folders, setFolders] = useState([]);
    const [files, setFiles] = useState([]);
    //사이드 메뉴 바 출력 여부
    const [menuOpen, setMenuOpen] = useState(false);
    //업로드 메뉴 출력 여부
    const [uploadMenuOpen, setUploadMenuOpen] = useState(false);
    //하단 파일 상세 메뉴 출력 여부
    const [fileMenuOpen, setFileMenuOpen] = useState(false);
    const [selectedFile, setSelectedFile] = useState({});

    const handleClickFile = (file) => {
        setFileMenuOpen(true);
        setSelectedFile(file);
    }
    //파일 추가 함수
    const addFile = (file) => {setFiles((p)=>[...p,file])}
    //폴더 추가 함수
    const addFolder = (folder) => {setFolders((p)=>[...p,folder])}

    //폴더안에 있는 폴더,파일을 가져오는 함수
    const getMyFileData = useCallback(async (folderCode) => {
        if (!folderCode) {
            const { data } = await api.get('/main/root-folder');
            folderCode = data;
        }
        
        const myFiles = await api.get(`/main/folder?folderCode=${folderCode}`);
                
        setFiles(myFiles.data.files);
        setFolders(myFiles.data.folders);
    }, []);
    //폴더 내부로 들어갈 때 호출되는 함수
    const intoFolder = (folderCode) => {      
        setUploadMenuOpen(false);  
        setFileMenuOpen(false);
        setHistory((prev)=>[...prev, folderCode]);
    }
    const back = () => {
        setUploadMenuOpen(false);  
        setFileMenuOpen(false);
        setHistory((prev)=> {
            if(prev.length === 0){
                return prev;
            } else {
                return prev.slice(0,-1);
            }
        })
    }
    useEffect(()=>{        
        if(history.length!==0){            
            getMyFileData(history[history.length - 1]);
        } else {
            getMyFileData();
        }
    },[history, getMyFileData])


    return (
        <div className={s.container}>

            <header className={s.header}>
                <div>내 파일</div>
                <div>
                    <button>돋보기</button>
                    {history.length!==0&&<button onClick={back}>뒤로가기</button>}
                    <button onClick={() => setMenuOpen(fileMenuOpen?false:!menuOpen)}>☰</button>
                </div>
            </header>

            <main className={s.main}>
                <section className={s.folderContainer}>
                    {folders.map((folder) => <Folder key={folder.folderCode} folder={folder} intoFolder={intoFolder}/>)}
                </section>
                <section className={s.fileContainer}>
                    {files.map((file) => <File onClick={handleClickFile} key={file.fileCode} file={file} />)}
                </section>
            </main>

            {/* 업로드 버튼 */}
            <UploadButton state={uploadMenuOpen} setState={setUploadMenuOpen} addFile={addFile} addFolder={addFolder}/>
            {/* 사이드 바 */}
            <Sidebar state={menuOpen}/>
            {/* 하단 파일 누르면 나오는 디테일 */}
            <FileDetailMenu file={selectedFile} setClose={setFileMenuOpen} state={fileMenuOpen}/>
            {/* 반투명 오버레이 */}
            {menuOpen && <div className={s.overlay} onClick={() => {setMenuOpen(false)}}></div>}
        </div>
    )
}

export default MobileMainUpdate;