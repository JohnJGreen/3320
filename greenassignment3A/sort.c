#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

void BubbleSort();

struct entry  {
	char *string;
	float mag;
};

int main(int argc, char *argv[])
{
	FILE *fp;
    char buff[1024], *tok;
    int count=0, lines=0, ch=0, i;
    struct entry **ls = NULL;

    fp = fopen(argv[0], "r");
    if (fp == NULL){
		printf("%s failed\n", argv[0]);
		return -1;
	}
         
	while(!feof(fp))
	{
		ch = fgetc(fp);
		if(ch == '\n')
		{
			lines++;
		}
	}
	fclose(fp);
	fp = fopen(argv[0], "r");

	ls = malloc((lines+1) * sizeof *ls);
	
    while ((fgets (buff, sizeof(buff), fp)) != NULL) {
		ls[count] = malloc(sizeof ls);
		ls[count]->string = malloc(strlen(buff)+1);
		strcpy(ls[count]->string, buff);
        tok = strtok(buff, ",");
		tok = strtok(NULL, ",");
		tok = strtok(NULL, ",");
		tok = strtok(NULL, ",");
		tok = strtok(NULL, ",");
		ls[count]->mag = atof(tok);
		count++;
    }
    fclose(fp);
    
    BubbleSort(ls,lines-1);
    
    fp = fopen(argv[0], "w+");
    for(i=0; i<lines; i++){
		fputs(ls[i]->string, fp);
	}  
	fclose(fp); 
    
	return 0;
}

void BubbleSort(struct entry **ls, int len){
	
	int i, j;
	struct entry *swap;
	
	if(ls[0]->string[0] == 't'){
		ls[0]->mag = 99;
	}
	
	for(i=0; i<len; i++){
		for(j=0; j<len-i; j++){
			if(ls[j]->mag < ls[j+1]->mag){
				swap = ls[j];
				ls[j] = ls[j+1];
				ls[j+1] = swap;
			}
		}
	}
}


